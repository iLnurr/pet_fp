package fs2ws.impl

import cats.effect.IO
import fs2ws.Domain._
import fs2ws.{JsonDecoder, JsonEncoder}
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import io.circe.parser._
import io.circe.syntax._

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
  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("type")
  implicit def encoder: JsonEncoder[IO, Message] = new JsonEncoder[IO, Message] {
    override def toJson(value: Message): IO[String] =
      IO.pure(value.asJson.noSpaces)
  }
}
