package fs2ws.impl.kafka

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.typesafe.scalalogging.StrictLogging
import fs2.Stream
import fs2ws.Domain.Message
import fs2ws.impl.MessageSerDe
import fs2ws.{Conf, MessageReader}

class KafkaReader[F[_]: ConcurrentEffect: ContextShift: Timer: Conf]
    extends MessageReader[F, Message]
    with StrictLogging {
  override def consume(): Stream[F, Message] =
    KafkaService
      .streamConsume(Conf[F].kafkaMessageTopic)
      .flatMap(
        ccr =>
          MessageSerDe.decodeMsg(ccr.record.value) match {
            case Some(value) =>
              logger.info(s"Consume message: $value")
              Stream.emit(value)
            case None => Stream.empty
          }
      )
}
