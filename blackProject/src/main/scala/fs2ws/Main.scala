package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import fs2ws.impl.JsonSerDe._
import cats.syntax.flatMap._
import fs2ws.impl.{MessageProcessorImpl, ServerImpl}

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit val mp = new MessageProcessorImpl()
  override def run(args: List[String]): IO[ExitCode] = {
    (new ServerImpl().startWS(9000)
      >> IO.pure(ExitCode.Success))
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
      )
  }
}
