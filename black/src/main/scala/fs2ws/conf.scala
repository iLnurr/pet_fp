package fs2ws

import com.typesafe.config.ConfigFactory

object conf {
  private lazy val config = ConfigFactory.load()

  lazy val port: Int = config.getInt("server.port")

  lazy val kafkaBootstrapServer: String =
    config.getString("kafka.bootstrap-servers")
  lazy val kafkaGroupId:      String = config.getString("kafka.group-id")
  lazy val kafkaMessageTopic: String = config.getString("kafka.message-topic")

  lazy val dbDriver = config.getString("db.driver")
  lazy val dbUrl    = config.getString("db.url")
  lazy val dbUser   = config.getString("db.user")
  lazy val dbPass   = config.getString("db.password")
}
