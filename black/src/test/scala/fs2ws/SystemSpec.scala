package fs2ws

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.syntax.either._
import com.dimafeng.testcontainers._
import com.typesafe.scalalogging.StrictLogging
import fs2.{Pipe, Stream}
import fs2ws.websocket.Helper._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import sttp.model.ws.WebSocketFrame
import Domain._
import fs2ws.impl.MessageSerDe._

import scala.concurrent.ExecutionContext

class SystemSpec
    extends FlatSpec
    with Matchers
    with BeforeAndAfterAll
    with StrictLogging {
  behavior.of("WebsocketServer")

  implicit val ec:    ExecutionContext     = ExecutionContext.Implicits.global
  implicit val cs:    ContextShift[IO]     = IO.contextShift(ec)
  implicit val timer: Timer[IO]            = IO.timer(ec)
  implicit val ce:    ConcurrentEffect[IO] = IO.ioConcurrentEffect

  val kafkaContainer      = new KafkaContainer()
  val postgreSQLContainer = new PostgreSQLContainer()
  val container: Container =
    MultipleContainers(kafkaContainer, postgreSQLContainer)
  lazy val kafkaBootstrapServers: String =
    kafkaContainer.container.getBootstrapServers
  lazy val postgresUrl: String = postgreSQLContainer.jdbcUrl

  it should "properly test" in {
    val receivePipe: Pipe[IO, String, Unit] =
      _.evalMap(m => IO(logger.info(s"Received $m")))
    val sendStream: Stream[IO, Either[WebSocketFrame.Close, String]] = Stream(
      encodeMsg(login("admin", "admin")).asRight,
      encodeMsg(subscribe_tables()).asRight,
      WebSocketFrame.close.asLeft
    )

    effect(send = sendStream, receivePipe = receivePipe).unsafeRunSync()

  }
  it should "properly register clients" in {}
  it should "properly authenticate clients" in {}
  it should "subscribe client" in {}
  it should "unsubscribe client" in {}
  it should "properly add table" in {}
  it should "properly update table" in {}
  it should "properly remove table" in {}

  override protected def beforeAll(): Unit = {
    container.start()
    Starter.start().map(_ => ()).unsafeRunAsyncAndForget()
  }

  override protected def afterAll(): Unit =
    container.stop()
}
