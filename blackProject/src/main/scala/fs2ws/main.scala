package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import cats.syntax.flatMap._

object main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] = {
    (server.start[IO].compile.drain
      >> IO.pure(ExitCode.Success))
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
      )
  }
}
