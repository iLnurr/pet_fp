package fs2ws.impl

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, Timer}
import com.typesafe.scalalogging.Logger
import fs2.Stream
import fs2ws.Domain._
import fs2ws._
import fs2ws.impl.State._
import scala.concurrent.duration._

class ServerImpl(
  val clients:  Clients[IO],
  val core:     MsgStreamPipe[IO] => Stream[IO, ExitCode],
  val services: Services[IO]
)(
  implicit ce:  ConcurrentEffect[IO],
  timer:        Timer[IO],
  contextShift: ContextShift[IO]
) extends Server[IO, Message, Message, MsgStreamPipe] {
  private val logger = Logger("ServerImpl")

  private val kafkaProducer = new MessageWriterImpl[IO]
  def kafkaConsumer(): Stream[IO, Message] =
    new MessageReaderImpl[IO].consume()

  override def handler: (Message, WSClient[IO]) => IO[Message] =
    (req, clientState) =>
      if (req.isInstanceOf[Command] && !clientState.privileged) {
        IO.pure(not_authorized())
      } else {
        services.handleReq(req).map {
          case Left(value) =>
            logger.error(value)
            empty
          case Right(value) =>
            value
        }
      }

  override def start(): IO[Unit] =
    core(pipe)
      .merge(processMsgFromKafka())
      .compile
      .drain

  override def pipe: MsgStreamPipe[IO] =
    input =>
      Stream
        .bracket(clients.register(Client()))(c => clients.unregister(c))
        .flatMap { client =>
          val bufferStream = Stream
            .awakeEvery[IO](5.seconds)
            .flatMap(_ => Stream.emits(client.take()))

          bufferStream.merge(processMsgFromWS(input, client))
        }

  private def processMsgFromWS(
    input:  Stream[IO, Message],
    client: WSClient[IO]
  ): Stream[IO, Message] =
    input.evalMap { request =>
      for {
        clientState <- clients.get(client.id).map(_.getOrElse(client))
        _ = logger.info(s"Got request: $request")
        response <- handler(request, clientState)
        _ = logger.info(s"Response $response")
        _ = updateStateAsync(clients, clientState, request, response)
      } yield response
    }

  def processMsgFromKafka(): Stream[IO, Unit] =
    kafkaConsumer().evalMap(
      msg =>
        clients
          .subscribed()
          .map(_.foreach(_.add(msg)))
    )

  private def updateStateAsync(
    clients:     Clients[IO],
    clientState: WSClient[IO],
    request:     Message,
    response:    Message
  ): Unit =
    updateState(clients, clientState, request, response)
      .unsafeRunAsyncAndForget()

  private def updateState(
    clients:     Clients[IO],
    clientState: WSClient[IO],
    request:     Message,
    response:    Message
  ): IO[Unit] =
    response match {
      case _: table_added | _: table_updated | _: table_removed =>
        services.tableList
          .flatMap { message =>
            logger.info(s"ConnectedClients: broadcast $message")
            kafkaProducer.send(message).compile.drain
          }
      case response =>
        clients
          .update(
            clientState
              .updateState(request)
              .updateState(response)
          )
    }
}
