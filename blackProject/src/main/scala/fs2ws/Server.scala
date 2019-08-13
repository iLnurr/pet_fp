package fs2ws

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.IO
import fs2._
import scodec.Attempt.{Failure, Successful}
import scodec.bits.ByteVector
import scodec.{Codec, Err}
import spinoco.fs2.http
import spinoco.fs2.http._
import spinoco.fs2.http.websocket.Frame
import spinoco.protocol.http._

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.{Concurrent, ContextShift, IO, Timer}

import scala.concurrent.ExecutionContext

import scala.concurrent.duration._


object Server extends App {

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

  def service(request: HttpRequestHeader, body: Stream[IO,Byte]): Stream[IO,HttpResponse[IO]] = {
    spinoco.fs2.http.websocket.server(wsPipe, 1.second)(request, body).onFinalize(IO.delay(println("WS DONE")))
  }

  def wsPipe: Pipe[IO, Frame[String], Frame[String]] = { inbound =>
    inbound
  }

  val serv = http.server[IO](new InetSocketAddress("127.0.0.1", 9000))(service)

  serv.compile.drain.unsafeRunSync()
}
