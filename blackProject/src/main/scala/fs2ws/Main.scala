package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import fs2ws.impl.JsonSerDe._
import cats.syntax.flatMap._
import fs2ws.impl.ServerImpl

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] = {
    (new ServerImpl(FS2Server.start).start(9000)
      >> IO.pure(ExitCode.Success))
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
      )
  }
}
