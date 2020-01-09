package fs2ws.impl

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import com.typesafe.scalalogging.Logger
import fs2ws.Domain.Message
import fs2ws.{conf, MessageWriter}
import fs2.Stream
import fs2.kafka.ProducerResult

class MessageWriterImpl[F[_]: ConcurrentEffect: ContextShift: Timer]
    extends MessageWriter[F, Message] {
  private val logger = Logger(getClass)
  type Result = ProducerResult[String, String, Unit]
  def send(msg: Message): Stream[F, Result] = {
    logger.info(s"Produce message: $msg")
    KafkaImpl.streamProduce(
      conf.kafkaMessageTopic,
      ("message", MessageSerDe.encodeMsg(msg))
    )
  }
}
