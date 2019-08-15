package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import cats.syntax.flatMap._
import spinoco.fs2.http.websocket.Frame
import spinoco.fs2.http.websocket.Frame.Text

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  def convert: Frame[String] => Frame[String] = { f =>
    println(s"Server got request: ${f.a}")
    Text(f.a)
  }
  override def run(args: List[String]): IO[ExitCode] = {
    (Server.start[IO](_.map(convert)).compile.drain
      >> IO.pure(ExitCode.Success))
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
      )
  }
}
