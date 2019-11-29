package fs2ws

import com.dimafeng.testcontainers.{KafkaContainer, PostgreSQLContainer}
import org.scalatest.{FlatSpec, Matchers}

class SystemSpec extends FlatSpec with Matchers {
  behavior.of("WebsocketServer")

  val kafkaContainer             = new KafkaContainer()
  val postgreSQLContainer        = new PostgreSQLContainer()
  lazy val kafkaBootstrapServers = kafkaContainer.container.getBootstrapServers
  lazy val postgresUrl           = postgreSQLContainer.jdbcUrl

  it should "properly register clients" in {}
  it should "properly authenticate clients" in {}
  it should "subscribe client" in {}
  it should "unsubscribe client" in {}
  it should "properly add table" in {}
  it should "properly update table" in {}
  it should "properly remove table" in {}
}
