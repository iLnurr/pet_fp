package fs2ws

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import fs2ws.Domain._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

class MessageProcessorAlgebraImpl extends MessageProcessorAlgebra[IO, Message, Message] {
  override def handler: Message => IO[Message] = {
    case req: AuthReq =>
      Services.auth(req)
    case req: PingReq =>
      Services.ping(req)
    case req: SubscribeTables =>
      Services.tables(req)
    case req: UnsubscribeTables =>
      Services.tables(req)
    case req: AddTableReq =>
      Services.tables(req)
    case req: UpdateTableReq =>
      Services.tables(req)
    case req: RemoveTableReq =>
      Services.tables(req)
  }
}
class FS2ServerImpl(implicit
                 cs: ContextShift[IO],
                 timer: Timer[IO],
                 ce: ConcurrentEffect[IO],
                 decoder: JsonDecoder[IO, Message],
                 encoder: JsonEncoder[IO, Message],
                 processor: MessageProcessorAlgebra[IO, Message, Message]
                ) extends ServerAlgebra[IO, Message, Message] {
  override def processEvent(raw: String)
                           (implicit
                            decoder: JsonDecoder[IO, Message],
                            encoder: JsonEncoder[IO, Message],
                            processor: MessageProcessorAlgebra[IO, Message, Message]): IO[String] = {
    for {
      msg <- decoder.fromJson(raw)
      responseMsg <- processor.handler(msg)
      responseString <- encoder.toJson(responseMsg)
    } yield responseString
  }

  override def startWS(port: Int): IO[Unit] =
    FS2Server
      .start[IO](s =>
        processEvent(s).unsafeRunSync()
      )
      .compile
      .drain
}

object JsonSerDe {
  private def handleError[A](either: Either[io.circe.Error, A]): IO[A] = either match {
    case Left(value) =>
      IO.raiseError(value)
    case Right(value) =>
      IO.pure(value)
  }
  implicit val incomingMessageDecoder: JsonDecoder[IO, Message] = new JsonDecoder[IO, Message] {
    override def fromJson(json: String): IO[Message] =
      for {
        msg <- handleError(decode[Msg](json))
        result <- handleError(msg.$type match {
          case MsgTypes.LOGIN =>
            decode[AuthReq](json)
          case MsgTypes.ADD_TABLE =>
            decode[AddTableReq](json)
          case MsgTypes.UPDATE_TABLE =>
            decode[UpdateTableReq](json)
          case MsgTypes.REMOVE_TABLE =>
            decode[RemoveTableReq](json)
          case MsgTypes.PING =>
            decode[PingReq](json)
          case MsgTypes.SUBSCRIBE_TABLE =>
            decode[SubscribeTables](json)
          case MsgTypes.UNSUBSCRIBE_TABLE =>
            decode[UnsubscribeTables](json)
        })
      } yield {
        result
      }
  }

  implicit def responseEncoder: JsonEncoder[IO, Message] = new JsonEncoder[IO, Message] {
    override def toJson(value: Message): IO[String] = value match {
      case msg: AuthMsg => msg match {
        case m: AuthReq =>
          IO.pure(m.asJson.toString())
        case m: AuthFailResp =>
          IO.pure(m.asJson.toString())
        case m: AuthSuccessResp =>
          IO.pure(m.asJson.toString())
      }
      case commands: Domain.PrivilegedCommands => commands match {
        case m: NotAuthorized =>
          IO.pure(m.asJson.toString())
        case m: UpdateTableFailResponse =>
          IO.pure(m.asJson.toString())
        case m: RemoveTableFailResponse =>
          IO.pure(m.asJson.toString())
        case m: TableAddedResponse =>
          IO.pure(m.asJson.toString())
        case m: RemoveTableResponse =>
          IO.pure(m.asJson.toString())
        case m: UpdateTableResponse =>
          IO.pure(m.asJson.toString())
      }
      case msg: TableMsg => msg match {
        case m: TableList =>
          IO.pure(m.asJson.toString())
        case m: UpdateTableFailResponse =>
          IO.pure(m.asJson.toString())
        case m: RemoveTableFailResponse =>
          IO.pure(m.asJson.toString())
        case m: TableAddedResponse =>
          IO.pure(m.asJson.toString())
        case m: RemoveTableResponse =>
          IO.pure(m.asJson.toString())
        case m: UpdateTableResponse =>
          IO.pure(m.asJson.toString())
      }
      case msg: PingMsg => msg match {
        case m: PongResponse =>
          IO.pure(m.asJson.toString())
      }
    }
  }
}
