package fs2ws.websocket

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import sttp.client._
import sttp.client.asynchttpclient.fs2.{
  AsyncHttpClientFs2Backend,
  Fs2WebSocketHandler,
  Fs2WebSockets
}
import fs2.{Pipe, Stream}
import sttp.client.ws._
import sttp.model.ws.WebSocketFrame

object Helper {
  def effect(
    uri:         String = "ws://localhost:9000/ws_api",
    send:        Stream[IO, Either[WebSocketFrame.Close, String]],
    receivePipe: Pipe[IO, String, Unit]
  )(
    implicit ce: ConcurrentEffect[IO],
    cs:          ContextShift[IO],
    t:           Timer[IO]
  ): IO[Unit] =
    AsyncHttpClientFs2Backend[IO]().flatMap { implicit backend =>
      basicRequest
        .get(uri"$uri")
        .openWebsocketF(Fs2WebSocketHandler())
        .flatMap { response =>
          Fs2WebSockets.handleSocketThroughTextPipe(response.result) { in =>
            send.merge(in.through(receivePipe).drain)
          }
        }
    }
}
