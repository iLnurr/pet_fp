package fs2ws

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.{Concurrent, ConcurrentEffect, Timer}
import fs2._
import scodec.Attempt.{Failure, Successful}
import scodec.bits.ByteVector
import scodec.{Codec, Err}
import spinoco.fs2.http
import spinoco.fs2.http._
import spinoco.fs2.http.websocket.Frame
import spinoco.protocol.http.Uri.Path
import spinoco.protocol.http._

import scala.concurrent.duration._

object FS2Server {
  implicit val AG: AsynchronousChannelGroup =
    AsynchronousChannelGroup
      .withThreadPool(Executors.newCachedThreadPool(util.mkThreadFactory("fs2-http-spec-AG", daemon = true)))

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

  def service[F[_]](request: HttpRequestHeader, body: Stream[F,Byte])
                   (wsPipe: Pipe[F, Frame[String], Frame[String]])
                   (implicit conc: Concurrent[F], timer: Timer[F]): Stream[F, HttpResponse[F]] = {
    request.path match {
      case Path(true, false, Seq("ws_api")) =>
        websocket
          .server[F,String,String](wsPipe, 1.second)(request, body)
          .onFinalize(conc.delay{println(s"WS DONE")})
      case other =>
        println(s"Client try to connect to path=${other.segments.mkString("/","/","")}. \nBad request=$request")
        Stream.empty
    }
  }

  def start[F[_]: ConcurrentEffect: Timer](wsPipe: Pipe[F, Frame[String], Frame[String]]): Stream[F, Unit] =
    http.server[F](new InetSocketAddress("127.0.0.1", 9000))(service(_, _)(wsPipe))
}