package fs2ws

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.{Concurrent, ConcurrentEffect, Timer}
import com.typesafe.config.ConfigFactory
import fs2._
import scodec.Codec
import spinoco.fs2.http
import spinoco.fs2.http._
import spinoco.fs2.http.websocket.Frame
import spinoco.protocol.http.Uri.Path
import spinoco.protocol.http._

import scala.concurrent.duration._

object FS2Server {
  lazy val config = ConfigFactory.load()
  lazy val port   = config.getInt("server.port")
  implicit val AG: AsynchronousChannelGroup =
    AsynchronousChannelGroup
      .withThreadPool(
        Executors.newCachedThreadPool(
          util.mkThreadFactory("fs2-http-spec-AG", daemon = true)
        )
      )

  implicit val codec: Codec[String] = scodec.codecs.utf8

  def service[F[_]](request: HttpRequestHeader, body: Stream[F, Byte])(
    wsPipe:                  Pipe[F, Frame[String], Frame[String]]
  )(implicit conc:           Concurrent[F], timer: Timer[F]): Stream[F, HttpResponse[F]] =
    request.path match {
      case Path(true, false, Seq("ws_api")) =>
        websocket
          .server[F, String, String](wsPipe, 1.second)(request, body)
          .onFinalize(conc.delay { println(s"WS DONE") })
      case other =>
        println(
          s"Client try to connect to path=${other.segments.mkString("/", "/", "")}. \nBad request=$request"
        )
        Stream.empty
    }

  def start[F[_]: ConcurrentEffect: Timer](
    wsPipe: Pipe[F, Frame[String], Frame[String]]
  ): Stream[F, Unit] =
    Stream.emit[F, Unit](println(s"Start server on port $port")) ++
    http.server[F](new InetSocketAddress("127.0.0.1", port))(
      service(_, _)(wsPipe)
    )
}
