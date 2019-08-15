package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import cats.syntax.flatMap._

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] = {
    (FS2Server.start[IO](identity).compile.drain
      >> IO.pure(ExitCode.Success))
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
      )
  }
}
