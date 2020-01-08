package fs2ws.impl

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.typesafe.scalalogging.StrictLogging
import fs2.Stream
import fs2ws.Domain.Message
import fs2ws.{conf, MessageReaderAlgebra}

class MessageReaderImpl[F[_]: ConcurrentEffect: ContextShift: Timer]
    extends MessageReaderAlgebra[F, Message]
    with StrictLogging {
  override def consume(): Stream[F, Message] =
    KafkaImpl
      .streamConsume(conf.kafkaMessageTopic)
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
