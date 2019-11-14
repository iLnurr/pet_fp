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
  override def handler: (Message,Client[IO]) => IO[Message] = (req, clientState) =>
    if (req.isInstanceOf[PrivilegedCommands] && !clientState.privileged) {
      IO.pure(not_authorized())
    } else {
      Services.handleReq(req)
    }

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
                response <- handler(request, clientState)
                _ = updateStateAsync(clients,clientState,request,response)
              } yield response
            }

            inputStream merge push(clients, client)
          }
      }

  private def updateStateAsync(clients: Clients[IO], clientState: Client[IO], request: Message , response: Message): Unit =
    updateState(clients,clientState,request,response).unsafeRunAsyncAndForget()

  private def updateState(clients: Clients[IO], clientState: Client[IO], request: Message , response: Message): IO[Unit] = {
    response match {
      case _: table_added | _: table_updated | _: table_removed =>
        Services.tableList
          .flatMap { tableList =>
            clients.broadcast(tableList, _.subscribed)
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
