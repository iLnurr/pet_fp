package fs2ws.impl

import cats.effect.{ConcurrentEffect, IO}
import fs2.Stream
import fs2ws.Domain._
import fs2ws._
import fs2ws.impl.State._
import spinoco.fs2.http.websocket.Frame
import spinoco.fs2.http.websocket.Frame.Text

class ServerImpl(core: FS2StreamPipe => Stream[IO,Unit])
                (implicit ce: ConcurrentEffect[IO],
                 decoder: JsonDecoder[IO, Message],
                 encoder: JsonEncoder[IO, Message])
  extends ServerAlgebra[IO, Message, Message, FS2StreamPipe] {
  val clients: IO[Clients[IO]] = ConnectedClients.create[IO]
  override def handler: Message => IO[Message] =
    Services.handleReq
  override def start(port: Int): IO[Unit] =
    core{
      pipe
    }.compile
      .drain

  override def pipe: FS2StreamPipe = input =>
    Stream
      .eval(clients)
      .flatMap{ clients =>
        Stream
          .bracket(clients.register(Client()))(c => clients.unregister(c))
          .flatMap{client =>
            input.evalMap(FS2Server.frameConvert{in =>
              decoder.fromJson(in)
                .flatMap(clients.updateClients(client,_)) // update by incoming
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
                }
            })
              .flatMap{ frame =>
                val (msg, updatedClient) = frame.a
                Stream.emits[IO, Frame[Message]](Text(msg) :: updatedClient.take.map(msg => Text(msg)))
              }.evalMap(frame => encoder.toJson(frame.a).map(Text(_)))
          }
      }
}
