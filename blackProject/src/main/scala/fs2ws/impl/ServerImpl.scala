package fs2ws.impl

import cats.effect.{ConcurrentEffect, IO, Timer}
import fs2.Stream
import fs2ws.Domain._
import fs2ws._
import fs2ws.impl.State._

class ServerImpl(val clients: Clients[IO],
                 val core: MsgStreamPipe[IO] => Stream[IO,Unit])
                (implicit ce: ConcurrentEffect[IO], timer: Timer[IO])
  extends ServerAlgebra[IO, Message, Message, MsgStreamPipe] {
  override def handler: Message => IO[Message] =
    Services.handleReq
  override def start(): IO[Unit] =
    core{
      pipe
    }.compile
      .drain

  override def pipe: MsgStreamPipe[IO] = input =>
    Stream
      .eval(IO.pure(clients))
      .flatMap{ clients =>
        Stream
          .bracket(clients.register(Client()))(c => clients.unregister(c))
          .flatMap{client =>
            val inputStream = input.evalMap { request =>
              for {
                clientState <- clients.get(client.id).map(_.getOrElse(client))
                response <- handler(request)
              } yield {
                if (request.isInstanceOf[PrivilegedCommands] && !clientState.privileged) {
                  NotAuthorized()
                } else {
                  response match {
                    case msg@(_: AddTableResponse | _: UpdateTableResponse | _: RemoveTableResponse) =>
                      Services.tableList
                        .flatMap { tableList =>
                          clients.broadcast(tableList, _.subscribed)
                        }.unsafeRunAsyncAndForget()
                      msg
                    case response =>
                      IO.delay {
                        val stateUpdatedByRequest = clientState.updateState(request)
                        val stateUpdatedByResponse = stateUpdatedByRequest.updateState(response)
                        stateUpdatedByResponse
                      }.flatMap(clients.update)
                        .unsafeRunAsyncAndForget()
                      response
                  }
                }
              }
            }

            inputStream merge push(clients, client)
          }
      }

  import scala.concurrent.duration._
  private def push(clients: Clients[IO], client: Client[IO]): Stream[IO, Message] = {
    val pushStream = Stream
      .awakeEvery[IO](5.seconds)
      .evalMap(_ =>
        clients.get(client.id).map {
          case Some(client) =>
            val messages = client.take
            if (messages.nonEmpty) {
              println(s"Push to client ${client} ${messages.mkString(",")}")
              Stream
                .emits[IO, Message](messages)
            } else {
              Stream.empty
            }
          case None =>
            Stream.empty
        }
      ).flatten

    pushStream
  }
}
