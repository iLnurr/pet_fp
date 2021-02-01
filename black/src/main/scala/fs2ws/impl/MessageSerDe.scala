package fs2ws.impl

import fs2ws.Domain._
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import io.circe.parser._
import io.circe.syntax._

object MessageSerDe {
  def decodeMsg: String  => Option[Message] = decode[Message](_).toOption
  def encodeMsg: Message => String          = _.asJson.noSpaces
  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("$type")
}
