import cats.effect.{ConcurrentEffect, Timer}
import fs2.{Pipe, Stream}
import fs2ws.Domain.Message
import spinoco.fs2.http.websocket.Frame
import spinoco.fs2.http.websocket.Frame.Text

package object fs2ws {
  type MsgStreamPipe[F[_]] = Pipe[F,Message, Message]
  type CorePipe[F[_]] = Pipe[F, Frame[String], Frame[String]]

  import cats.syntax.functor._
  import cats.instances.function._
  def convert[F[_]](encode: Message => F[String],
                    decode: String => F[Message],
                    msgStreamPipe: MsgStreamPipe[F]): CorePipe[F] =
    msgStreamPipe
      .compose[Stream[F,Frame[String]]]{ stream =>
        stream.evalMap{frame =>
          println(s"Server got request: ${frame.a}")
          decode(frame.a)
        }
      }.map{ response =>
      response.evalMap(encode).map{s =>
        println(s"Server response: $s")
        Text(s)
      }
    }

  def startMsgStream[F[_]: ConcurrentEffect: Timer](encode: Message => F[String],
                                                    decode: String => F[Message],
                                                    msgStreamPipe: MsgStreamPipe[F]): Stream[F, Unit] =
    FS2Server.start[F](convert(encode,decode,msgStreamPipe))
}
