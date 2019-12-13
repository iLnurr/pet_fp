package fs2ws.impl

import cats.effect.{
  Concurrent,
  ConcurrentEffect,
  ContextShift,
  Resource,
  Sync,
  Timer
}
import cats.syntax.flatMap._
import com.typesafe.scalalogging.Logger
import fs2.Stream
import fs2.kafka._
import fs2ws.conf._
import org.apache.kafka.clients.admin.NewTopic

object KafkaImpl {
  private val logger = Logger(getClass)
  private def adminClientSettings[F[_]: Sync](
    bootstrapServers: String
  ): AdminClientSettings[F] =
    AdminClientSettings[F].withBootstrapServers(bootstrapServers)

  def kafkaAdminClientResource[F[_]: Concurrent: ContextShift](
    bootstrapServers: String
  ): Resource[F, KafkaAdminClient[F]] =
    adminClientResource(adminClientSettings[F](bootstrapServers))

  def createTopic[F[_]: Concurrent: ContextShift]: F[Unit] =
    kafkaAdminClientResource[F](kafkaBootstrapServer).use { client =>
      client.createTopic(new NewTopic(kafkaMessageTopic, 1, 1))
    }

  private def consumerSettings[F[_]: Sync]
    : ConsumerSettings[F, String, String] =
    ConsumerSettings[F, String, String]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(kafkaBootstrapServer)
      .withGroupId(kafkaGroupId)

  private def producerSettings[F[_]: Sync]
    : ProducerSettings[F, String, String] =
    ProducerSettings[F, String, String]
      .withBootstrapServers(kafkaBootstrapServer)

  def streamConsume[F[_]: ConcurrentEffect: ContextShift: Timer](
    topic: String
  ): Stream[F, CommittableConsumerRecord[F, String, String]] = {
    logger.info(s"Consume msgs from: $topic")
    consumerStream[F]
      .using(consumerSettings)
      .evalTap(_.subscribeTo(topic))
      .flatMap(_.stream)
  }

  def streamProduce[F[_]: ConcurrentEffect: ContextShift](
    topic:  String,
    record: (String, String)
  ): Stream[F, ProducerResult[String, String, Unit]] = {
    logger.info(s"Produce record: $record")
    producerStream[F]
      .using(producerSettings)
      .evalMap { producer =>
        val rs =
          ProducerRecords.one(ProducerRecord(topic, record._1, record._2))
        producer.produce(rs).flatten
      }
  }
}
