package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import cats.syntax.flatMap._
import fs2.Stream
import fs2ws.Domain.{AuthReq, Message}
import fs2ws.impl.JsonSerDe._
import fs2ws.impl.ServerImpl

object ServerTest extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  val authReq = AuthReq("admin", "admin")
  override def run(args: List[String]): IO[ExitCode] = {
    val server = new ServerImpl(startMsgStream(encoder.toJson, incomingMessageDecoder.fromJson, _))
    implicit val serverPipe: MsgStreamPipe[IO] = server.pipe

    test(authReq, Seq())(serverPipe)
  }

  def test(input: Message, expected: Seq[Message])(implicit pipe: MsgStreamPipe[IO]): IO[ExitCode] = {
    val inStream = Stream.emit[IO,Message](input)
    val resultStream = pipe(inStream)
    println(expected)
    resultStream
      .compile
      .toList >> IO.pure(ExitCode.Success)
  }
}
