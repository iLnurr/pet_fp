package fs2ws

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.{Concurrent, ContextShift, ExitCode, IO, IOApp, Timer}
import cats.syntax.flatMap._
import fs2.{Pipe, Stream}
import fs2ws.Domain.{AddTableReq, AuthReq, Message, SubscribeTables, Table, UnsubscribeTables}
import fs2ws.impl.JsonSerDe._
import fs2ws.impl.ServerImpl
import scodec.Attempt.{Failure, Successful}
import scodec.bits.ByteVector
import scodec.{Codec, Err}
import spinoco.fs2.http._
import spinoco.fs2.http.websocket.Frame.Text
import spinoco.fs2.http.websocket.{Frame, WebSocket, WebSocketRequest}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ServerTest extends IOApp {
  implicit val _cxs: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
  implicit val _timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
  implicit val _concurrent: Concurrent[IO] = IO.ioConcurrentEffect(_cxs)
  implicit val AG: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool(util.mkThreadFactory("fs2-http-spec-AG", daemon = true)))
  implicit val codec: Codec[String] = scodec.codecs.bytes.exmap[String](
    bv => bv.decodeUtf8 match {
      case Left(exception) => Failure(Err(exception.getMessage))
      case Right(str) => Successful(str)
    }
    , b => ByteVector.encodeUtf8(b) match {
      case Left(exception) => Failure(Err(exception.getMessage))
      case Right(vector) => Successful(vector)
    }
  )
  val authReq = AuthReq("admin", "admin")
  val subscribeTables = SubscribeTables()
  val unsubscribeTables = UnsubscribeTables()
  val addTableReq = AddTableReq(1, Table(None, "ff", 0))

  override def run(args: List[String]): IO[ExitCode] = {
    val clientStream = Stream.sleep_[IO](3.seconds) ++ WebSocket.client(
      WebSocketRequest.ws("localhost",9000, "/ws_api")
      , wspipe(Seq(authReq, subscribeTables, unsubscribeTables, addTableReq))
    ).map { x =>
      println(("RESULT OF WS", x))
    }
    val serverStream = new ServerImpl(startMsgStream(encoder.toJson, incomingMessageDecoder.fromJson, _))

    val resultClient =
      (serverStream.core(serverStream.pipe) mergeHaltBoth clientStream).compile.drain

    (resultClient >> IO.pure(ExitCode.Success))
      .handleErrorWith(ex =>
        IO {
          println(s"Server stopped with error ${ex.getMessage}")
          ExitCode.Error
        }
      )
  }

  def wspipe(msgs: Seq[Message]): Pipe[IO, Frame[String], Frame[String]] = { _ =>
    val seq = msgs.map(encoder.toJson(_).unsafeRunSync())
    val output =  Stream.emits(seq).map(Text(_))
    output
  }
}
