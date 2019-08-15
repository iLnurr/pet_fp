package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import JsonSerDe._
import cats.syntax.flatMap._

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit val mp = new MessageProcessorAlgebraImpl()
  override def run(args: List[String]): IO[ExitCode] = {
    (new FS2ServerImpl().startWS(9000) // TODO state of userType
      >> IO.pure(ExitCode.Success))
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
      )
  }
}
