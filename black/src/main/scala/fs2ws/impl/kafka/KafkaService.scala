package fs2ws.impl.kafka

import cats.effect._
import cats.implicits._
import fs2.kafka._
import com.typesafe.scalalogging.Logger
import fs2.Stream
import fs2ws.Conf
import org.apache.kafka.clients.admin.NewTopic

object KafkaService {
  private val logger = Logger(getClass)
  private def adminClientSettings[F[_]: Sync](
      bootstrapServers: String
  ): AdminClientSettings[F] =
    AdminClientSettings[F].withBootstrapServers(bootstrapServers)

  def kafkaAdminClientResource[F[_]: Concurrent: ContextShift](
      bootstrapServers: String
  ): Resource[F, KafkaAdminClient[F]] =
    adminClientResource(adminClientSettings[F](bootstrapServers))

  def createTopic[F[_]: Concurrent: ContextShift: Conf]: F[Unit] =
    kafkaAdminClientResource[F](Conf[F].kafkaBootstrapServer).use { client =>
      client.createTopic(new NewTopic(Conf[F].kafkaMessageTopic, 1, 1.toShort))
    }

  private def consumerSettings[F[_]: Sync: Conf]: ConsumerSettings[F, String, String] =
    ConsumerSettings[F, String, String]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(Conf[F].kafkaBootstrapServer)
      .withGroupId(Conf[F].kafkaGroupId)

  private def producerSettings[F[_]: Sync: Conf]: ProducerSettings[F, String, String] =
    ProducerSettings[F, String, String]
      .withBootstrapServers(Conf[F].kafkaBootstrapServer)

  def streamConsume[F[_]: ConcurrentEffect: ContextShift: Timer: Conf](
      topic: String
  ): Stream[F, CommittableConsumerRecord[F, String, String]] = {
    logger.info(s"Consume from topic: `$topic``")
    consumerStream[F]
      .using(consumerSettings[F])
      .evalTap(_.subscribeTo(topic))
      .flatMap(_.stream)
  }

  def streamProduce[F[_]: ConcurrentEffect: ContextShift: Conf](
      topic: String,
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
