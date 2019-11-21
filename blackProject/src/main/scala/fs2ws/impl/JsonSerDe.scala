package fs2ws.impl

import cats.effect.IO
import fs2ws.Domain._
import fs2ws.{JsonDecoder, JsonEncoder}
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.parser._
import io.circe.syntax._

object JsonSerDe {
  private def handleError[A](either: Either[io.circe.Error, A]): IO[A] =
    either match {
      case Left(value) =>
        IO.raiseError(value)
      case Right(value) =>
        IO.pure(value)
    }
  implicit val incomingMessageDecoder: JsonDecoder[IO, Message] =
    new JsonDecoder[IO, Message] {
      override def fromJson(json: String): IO[Message] =
        handleError(decode[Message](json))
    }
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("$type")
  implicit def encoder: JsonEncoder[IO, Message] =
    new JsonEncoder[IO, Message] {
      override def toJson(value: Message): IO[String] =
        IO.pure(value.asJson.noSpaces)
    }
}
