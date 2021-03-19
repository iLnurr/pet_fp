package fs2ws.impl.kafka

import cats.effect.{ ConcurrentEffect, ContextShift, Timer }
import com.typesafe.scalalogging.Logger
import fs2.Stream
import fs2.kafka.ProducerResult
import fs2ws.Domain.Message
import fs2ws.impl.MessageSerDe
import fs2ws.{ Conf, MessageWriter }

class KafkaWriter[F[_]: ConcurrentEffect: ContextShift: Timer: Conf] extends MessageWriter[F, Message] {
  private val logger = Logger(getClass)
  type Result = ProducerResult[String, String, Unit]
  def send(msg: Message): Stream[F, Result] = {
    logger.info(s"Produce message: $msg")
    KafkaService.streamProduce(
      Conf[F].kafkaMessageTopic,
      ("message", MessageSerDe.encodeMsg(msg))
    )
  }
}
