package fs2ws.impl

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import fs2ws.Conf

class ConfImpl extends Conf[IO] {
  private val config = ConfigFactory.load()

  lazy val port: Int = config.getInt("server.port")

  lazy val kafkaBootstrapServer: String =
    config.getString("kafka.bootstrap-servers")
  lazy val kafkaGroupId:      String = config.getString("kafka.group-id")
  lazy val kafkaMessageTopic: String = config.getString("kafka.message-topic")

  lazy val dbDriver: String = config.getString("db.driver")
  lazy val dbUrl:    String = config.getString("db.url")
  lazy val dbUser:   String = config.getString("db.user")
  lazy val dbPass:   String = config.getString("db.password")
}
