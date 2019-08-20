package fs2ws.impl

import cats.effect.{ConcurrentEffect, IO}
import fs2.Stream
import fs2ws.Domain._
import fs2ws._
import fs2ws.impl.State._

class ServerImpl(val core: MsgStreamPipe[IO] => Stream[IO,Unit])
                (implicit ce: ConcurrentEffect[IO])
  extends ServerAlgebra[IO, Message, Message, MsgStreamPipe] {
  val clients: IO[Clients[IO]] = ConnectedClients.create[IO]
  override def handler: Message => IO[Message] =
    Services.handleReq
  override def start(): IO[Unit] =
    core{
      pipe
    }.compile
      .drain

  override def pipe: MsgStreamPipe[IO] = input =>
    Stream
      .eval(clients)
      .flatMap{ clients =>
        Stream
          .bracket(clients.register(Client()))(c => clients.unregister(c))
          .flatMap{client =>
            input.evalMap(in =>
              clients.updateClients(client,in) // update by incoming
                .flatMap { case (message, updated) => message match {
                  case commands: PrivilegedCommands =>
                    if (!updated.privileged) IO.pure(NotAuthorized() -> updated) else handler(commands).map(_ -> updated)
                  case other =>
                    handler(other).map(_ -> updated)
                }}
                .flatMap { case (msg, updated) =>
                  msg match {
                    case _: AddTableResponse | _: UpdateTableResponse | _: RemoveTableResponse =>
                      Services.tableList
                        .flatMap { tableList => clients.broadcast(tableList, _.subscribed) }
                        .map(_ => msg -> updated)
                    case _ =>
                      IO.pure(msg -> updated)
                  }
                }
                .flatMap{ case (msg,updated) =>
                  clients.updateClients(updated,msg) // update by response
                })
              .flatMap{ case (msg,updated) =>
                Stream.emits[IO, Message](msg :: updated.take.map(msg => msg))
              }
          }
      }
}
