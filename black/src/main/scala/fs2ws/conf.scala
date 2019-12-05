package fs2ws

import com.typesafe.config.ConfigFactory

object conf {
  private lazy val config = ConfigFactory.load()

  lazy val port: Int = config.getInt("server.port")

  lazy val kafkaBootstrapServer: String =
    config.getString("kafka.bootstrap-servers")
  lazy val kafkaGroupId:      String = config.getString("kafka.group-id")
  lazy val kafkaMessageTopic: String = config.getString("kafka.message-topic")
}
