package fs2ws.impl

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import fs2.Stream
import fs2ws.Domain._
import fs2ws._
import fs2ws.impl.State._

class ServerImpl(
  val clients:  Clients[IO],
  val core:     MsgStreamPipe[IO] => Stream[IO, Unit],
  val services: Services[IO]
)(
  implicit ce:  ConcurrentEffect[IO],
  timer:        Timer[IO],
  contextShift: ContextShift[IO]
) extends ServerAlgebra[IO, Message, Message, MsgStreamPipe] {
  override def handler: (Message, ClientAlgebra[IO]) => IO[Message] =
    (req, clientState) =>
      if (req.isInstanceOf[Command] && !clientState.privileged) {
        IO.pure(not_authorized())
      } else {
        services.handleReq(req)
      }

  override def start(): IO[Unit] =
    core {
      pipe
    }.compile.drain

  override def pipe: MsgStreamPipe[IO] =
    input =>
      Stream
        .eval(IO.pure(clients))
        .flatMap { clients =>
          Stream
            .bracket(clients.register(Client()))(c => clients.unregister(c))
            .flatMap { client =>
              val inputStream = input.evalMap { request =>
                for {
                  clientState <- clients.get(client.id).map(_.getOrElse(client))
                  response    <- handler(request, clientState)
                  _ = updateStateAsync(clients, clientState, request, response)
                } yield response
              }

              inputStream.merge(client.msgs)
            }
        }

  private def updateStateAsync(
    clients:     Clients[IO],
    clientState: ClientAlgebra[IO],
    request:     Message,
    response:    Message
  ): Unit =
    updateState(clients, clientState, request, response)
      .unsafeRunAsyncAndForget()

  private def updateState(
    clients:     Clients[IO],
    clientState: ClientAlgebra[IO],
    request:     Message,
    response:    Message
  ): IO[Unit] =
    response match {
      case _: table_added | _: table_updated | _: table_removed =>
        services.tableList
          .flatMap { tableList =>
            clients.broadcast(tableList)
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
