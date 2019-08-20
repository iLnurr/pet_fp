package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import cats.syntax.flatMap._
import fs2.Pipe
import fs2.Stream
import fs2ws.Domain.{AuthReq, Message}
import spinoco.fs2.http.websocket.Frame
import fs2ws.impl.JsonSerDe._
import fs2ws.impl.ServerImpl
import spinoco.fs2.http.websocket.Frame.Text

object ServerTest extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  val authReq = AuthReq("admin", "admin")
  override def run(args: List[String]): IO[ExitCode] = {
    val server = new ServerImpl(FS2Server.start)
    implicit val serverPipe: Pipe[IO, Frame[String], Frame[String]] = server.pipe

    test(authReq, Seq())(serverPipe)
  }

  def test(input: Message, expected: Seq[Message])(implicit pipe: Pipe[IO,Frame[String], Frame[String]]): IO[ExitCode] = {
    val inStream = Stream.eval[IO,Frame[String]](encoder.toJson(input).map(Text(_)))
    val resultStream = pipe(inStream)
    println(expected)
    resultStream
      .compile
      .toList >> IO.pure(ExitCode.Success)
  }
}
