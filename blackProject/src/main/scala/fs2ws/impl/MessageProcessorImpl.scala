package fs2ws.impl

import cats.effect.IO
import fs2ws.Domain.Message
import fs2ws.{MessageProcessorAlgebra, Services}

class MessageProcessorImpl extends MessageProcessorAlgebra[IO, Message, Message] {
  override def handler: Message => IO[Message] =
    Services.handleReq
}
