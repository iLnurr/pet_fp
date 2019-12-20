package fs2ws.websocket

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.syntax.either._
import com.typesafe.scalalogging.StrictLogging
import fs2ws.impl.MessageSerDe._
import sttp.client._
import sttp.client.asynchttpclient.fs2.{
  AsyncHttpClientFs2Backend,
  Fs2WebSocketHandler,
  Fs2WebSockets
}
import fs2.{Pipe, Stream}
import fs2ws.Domain.{ping, pong, Message}
import sttp.client.ws._
import sttp.model.ws.WebSocketFrame

import scala.util.Random

object Helper extends StrictLogging {
  def testWebsockets(
    msgsToSend:         List[Message],
    expected:           List[Message],
    additionalChecking: Message => Unit = _ => ()
  )(
    implicit ce: ConcurrentEffect[IO],
    cs:          ContextShift[IO],
    t:           Timer[IO]
  ): IO[Unit] = {
    val randomL          = Random.nextLong()
    val expectedWithPong = pong(randomL) :: expected
    val receivePipe: Pipe[IO, String, Unit] =
      _.evalMap(
        m =>
          IO {
            logger.info(s"Try to check $m")
            decodeMsg(m).foreach { mm =>
              assert(
                expectedWithPong.contains(mm),
                s"Expected \n $expectedWithPong \nshould contain $m"
              )
              additionalChecking(mm)
            }
          }
      )
    val toSend =
      Stream.emits(
        (msgsToSend ++ List(ping(randomL)))
          .map(encodeMsg(_).asRight[WebSocketFrame.Close])
      )
    val sendStream
      : Stream[IO, Either[WebSocketFrame.Close, String]] = toSend ++ Stream(
        WebSocketFrame.close.asLeft
      )
    effect(send = sendStream, receivePipe = receivePipe)
  }
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
