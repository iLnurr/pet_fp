package fs2ws

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import fs2._
import fs2ws.impl.MessageSerDe
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.websocket._
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame._

object Http4sWebsocketServer {
  private val logger = Logger(getClass)
  def start[F[_]: ConcurrentEffect: ContextShift: Timer](
    wsPipe: MsgStreamPipe[F]
  ): Stream[F, ExitCode] = new WebSocketServer[F](wsPipe).start

  final class WebSocketServer[F[_]: ConcurrentEffect: ContextShift: Timer](
    wsPipe: MsgStreamPipe[F]
  ) extends Http4sDsl[F] {

    def start: Stream[F, ExitCode] =
      BlazeServerBuilder[F]
        .bindHttp(conf.port)
        .withWebSockets(true)
        .withHttpApp(routes.orNotFound)
        .serve

    private[this] val routes: HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root / "ws_api" => {
        for {
          channel <- cats.effect.concurrent.MVar[F].empty[List[WebSocketFrame]]
          webSocket <- {
            WebSocketBuilder[F].build(
              send = fs2.Stream
                .eval(channel.take)
                .flatMap(fs2.Stream.emits(_))
                .repeat,
              receive = stream => {
                stream
                  .flatMap {
                    case text: Text =>
                      logger.info(s"Received frame:$text")
                      MessageSerDe.decodeMsg(text.str) match {
                        case Some(value) =>
                          logger.info(s"Emit message:$value")
                          Stream.emit(value)
                        case None =>
                          logger.warn(s"Can't decode $text")
                          Stream.emit(Domain.empty)
                      }
                    case other =>
                      logger.warn(s"Not known frame:$other")
                      Stream.emit(Domain.empty)
                  }
                  .through(wsPipe)
                  .evalMap { response =>
                    logger.info(s"Send response:$response")
                    channel
                      .put(List(Text(MessageSerDe.encodeMsg(response))))
                  }
              }
            )
          }
        } yield webSocket
      }
    }
  }
}
