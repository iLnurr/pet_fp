package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import fs2ws.impl.JsonSerDe._
import fs2ws.impl.ServerImpl
import fs2ws.impl.State.ConnectedClients

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      clients <- ConnectedClients.create[IO]
      _ <- new ServerImpl(clients, startMsgStream(encoder.toJson, incomingMessageDecoder.fromJson, _)).start()
    } yield {
      ExitCode.Success
    })
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
      )
  }
}
