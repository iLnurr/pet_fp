package fs2ws

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors
import io.circe.generic.auto._, io.circe.syntax._, io.circe.parser._
import cats.effect.{Concurrent, ConcurrentEffect, IO, Timer}
import fs2._
import fs2ws._
import scodec.Attempt.{Failure, Successful}
import scodec.bits.ByteVector
import scodec.{Codec, Err}
import spinoco.fs2.http
import spinoco.fs2.http._
import spinoco.fs2.http.websocket.Frame
import spinoco.fs2.http.websocket.Frame.Text
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
        spinoco.fs2.http.websocket.server[F,String,String](wsPipe, 1.second)(request, body)
          .onFinalize(conc.delay(println("WS DONE")))
      case other =>
        Stream.emit{
          println(s"Client try to connect to path=${other.segments.mkString("/","/","")}. \nBad request=$request")
          HttpResponse(HttpStatusCode.NotFound)
        }
    }
  }

  def start[F[_]: ConcurrentEffect: Timer](convertF: String => String = identity): Stream[F, Unit] = {
    http.server[F](new InetSocketAddress("127.0.0.1", 9000))(service(_,_)(_.map(frameConvert(convertF))))
  }
  def frameConvert(func: String => String): Frame[String] => Frame[String] = { f =>
    println(s"Server got request: ${f.a}")
    Text(func(f.a))
  }
}