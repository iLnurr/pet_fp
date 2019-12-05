package fs2ws.impl

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import fs2ws.Domain.Message
import fs2ws.{conf, MessageWriterAlgebra}
import fs2.Stream
import fs2.kafka.ProducerResult

class MessageWriterImpl[F[_]: ConcurrentEffect: ContextShift: Timer]
    extends MessageWriterAlgebra[F, Message] {
  type Result = ProducerResult[String, String, Unit]
  def send(msg: Message): Stream[F, Result] =
    KafkaImpl.streamProduce(
      conf.kafkaMessageTopic,
      ("message", MessageSerDe.encodeMsg(msg))
    )
}
