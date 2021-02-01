package fs2ws.impl

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, Sync, Timer}
import com.typesafe.scalalogging.Logger
import fs2.Stream
import fs2ws.Domain._
import fs2ws._
import fs2ws.impl.State._

import scala.concurrent.duration._
import cats.syntax.all._
import fs2ws.impl.kafka.{KafkaReader, KafkaWriter}

class ServerImpl[F[_]: ConcurrentEffect: ContextShift: Timer: Conf: Clients: MessageService](
  val core: MsgStreamPipe[F] => Stream[F, ExitCode]
) extends Server[F, Message, Message, MsgStreamPipe] {
  private val logger = Logger("ServerImpl")

  private val kafkaProducer = new KafkaWriter[F]
  def kafkaConsumer(): Stream[F, Message] =
    new KafkaReader[F].consume()

  override def start(): F[Unit] =
    core(pipe)
      .merge(processMsgFromKafka())
      .compile
      .drain

  override def pipe: MsgStreamPipe[F] =
    input =>
      Stream
        .bracket(Clients[F].register(Client()))(c => Clients[F].unregister(c))
        .flatMap { client =>
          val bufferStream = Stream
            .awakeEvery[F](5.seconds)
            .flatMap(_ => Stream.emits(client.take()))

          bufferStream.merge(processMsgFromWS(input, client))
        }

  private def processMsgFromWS(
    input:  Stream[F, Message],
    client: WSClient[F]
  ): Stream[F, Message] =
    input.evalMap { request =>
      for {
        clientState <- Clients[F].get(client.id).map(_.getOrElse(client))
        _ = logger.info(s"Got request: $request")
        response <- handler(request, clientState)
        _ = logger.info(s"Response $response")
        _ = updateStateAsync(clientState, request, response)
      } yield response
    }

  override def handler: (Message, WSClient[F]) => F[Message] =
    (req, clientState) =>
      if (req.isInstanceOf[Command] && !clientState.privileged) {
        Sync[F].pure(not_authorized())
      } else {
        MessageService[F].process(req).map {
          case Left(value) =>
            logger.error(value)
            empty
          case Right(value) =>
            value
        }
      }

  def processMsgFromKafka(): Stream[F, Unit] =
    kafkaConsumer().evalMap(
      msg =>
        Clients[F]
          .subscribed()
          .map(_.foreach(_.add(msg)))
    )

  private def updateStateAsync(
    clientState: WSClient[F],
    request:     Message,
    response:    Message
  ): Unit =
    ConcurrentEffect[F]
      .runAsync(
        updateState(Clients[F], clientState, request, response)
      )(_ => IO.unit)
      .unsafeRunSync()

  private def updateState(
    clients:     Clients[F],
    clientState: WSClient[F],
    request:     Message,
    response:    Message
  ): F[Unit] =
    response match {
      case _: table_added | _: table_updated | _: table_removed =>
        MessageService[F].tableList
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
