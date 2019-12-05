package fs2ws

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats.effect.{Concurrent, ConcurrentEffect, Timer}
import com.typesafe.scalalogging.Logger
import fs2._
import scodec.Codec
import spinoco.fs2.http
import spinoco.fs2.http._
import spinoco.fs2.http.websocket.Frame
import spinoco.protocol.http.Uri.Path
import spinoco.protocol.http._

import scala.concurrent.duration._

object FS2Server {
  private lazy val logger = Logger("FS2-Server")
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
          .onFinalize(conc.delay { logger.info(s"WS DONE") })
      case other =>
        logger.warn(
          s"Client try to connect to path=${other.segments.mkString("/", "/", "")}. \nBad request=$request"
        )
        Stream.empty
    }

  def start[F[_]: ConcurrentEffect: Timer](
    wsPipe: Pipe[F, Frame[String], Frame[String]]
  ): Stream[F, Unit] =
    Stream.emit[F, Unit](logger.info(s"Start server on port ${conf.port}")) ++
    http.server[F](new InetSocketAddress("127.0.0.1", conf.port))(
      service(_, _)(wsPipe)
    )
}
