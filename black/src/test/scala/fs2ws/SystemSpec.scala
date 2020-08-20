package fs2ws

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.syntax.either._
import cats.syntax.option._
import com.dimafeng.testcontainers._
import com.typesafe.scalalogging.StrictLogging
import fs2.{Pipe, Stream}
import fs2ws.websocket.Helper._
import org.scalatest.BeforeAndAfterAll
import sttp.model.ws.WebSocketFrame
import Domain._
import fs2ws.impl.{ConfImpl, MessageServiceImpl}
import fs2ws.impl.MessageSerDe._
import fs2ws.impl.State.ConnectedClients
import fs2ws.impl.doobie.{
  DoobieService,
  TableReader,
  TableWriter,
  UserReader,
  UserWriter
}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext

class SystemSpec
    extends AnyFlatSpec
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

  implicit val conf: Conf[IO] = new ConfImpl {
    override lazy val dbUrl: String = postgresUrl
  }
  implicit val clients: Clients[IO] =
    ConnectedClients.create[IO].unsafeRunSync()
  implicit val db = new DoobieService[IO]
  implicit val userReader:     UserReader[IO]     = new UserReader[IO]
  implicit val userWriter:     UserWriter[IO]     = new UserWriter[IO]
  implicit val tableReader:    TableReader[IO]    = new TableReader[IO]
  implicit val tableWriter:    TableWriter[IO]    = new TableWriter[IO]
  implicit val messageService: MessageService[IO] = new MessageServiceImpl[IO]

  it should "properly test" in {
    val receivePipe: Pipe[IO, String, Unit] =
      _.evalMap(m => IO(logger.info(s"Received $m")))
    val sendStream: Stream[IO, Either[WebSocketFrame.Close, String]] = Stream(
      encodeMsg(login("admin", "admin")).asRight,
      encodeMsg(subscribe_tables()).asRight,
      WebSocketFrame.close.asLeft
    )

    effect(send = sendStream, receivePipe = receivePipe).unsafeRunSync()

    testWebsockets(
      msgsToSend = List(login("admin", "admin"), subscribe_tables()),
      expected   = List(login_successful(UserType.ADMIN), table_list(Seq()))
    ).unsafeRunSync()

    testWebsockets(
      msgsToSend = List(login("admin", "admin"), subscribe_tables()),
      expected   = List() // it must not be empty
    ).handleErrorWith(
        throwable =>
          if (throwable.isInstanceOf[AssertionError]) {
            IO.unit
          } else {
            throw throwable
          }
      )
      .unsafeRunSync()
  }
  it should "properly register clients" in {
    testWebsockets(
      msgsToSend = List(login("admin", "admin"), ping(2)),
      expected   = List(login_successful(UserType.ADMIN), pong(2))
    ).unsafeRunSync()
  }
  it should "properly authenticate clients" in {
    (for {
      _ <- testWebsockets(
        msgsToSend = List(login("admin", "admin")),
        expected   = List(login_successful("admin"))
      )
      _ <- testWebsockets(
        msgsToSend = List(login("un", "upwd")),
        expected   = List(login_successful(UserType.USER))
      )
      _ <- testWebsockets(
        msgsToSend = List(login("unknown", "unknown")),
        expected   = List(login_failed())
      )
    } yield ()).unsafeRunSync()
  }
  it should "subscribe/unsubscribe client" in {
    testWebsockets(
      msgsToSend =
        List(login("admin", "admin"), subscribe_tables(), unsubscribe_tables()),
      expected =
        List(login_successful(UserType.ADMIN), table_list(Seq()), Domain.empty)
    ).unsafeRunSync()
  }
  it should "properly add table" in {
    val t  = Table(id = 1L.some, name = "test", participants  = 0)
    val t2 = Table(id = 2L.some, name = "test2", participants = 0)
    testWebsockets(
      msgsToSend =
        List(login("admin", "admin"), add_table(-1, t), add_table(0, t2)),
      expected = List(
        login_successful("admin"),
        table_added(-1, t),
        table_added(0, t2)
      )
    ).unsafeRunSync()
  }
  it should "properly update table" in {
    val t       = Table(id    = 3L.some, name = "test3", participants = 0)
    val updated = t.copy(name = "updated")
    testWebsockets(
      msgsToSend =
        List(login("admin", "admin"), add_table(-1, t), update_table(updated)),
      expected = List(
        login_successful("admin"),
        table_added(-1, t),
        table_updated(updated)
      )
    ).unsafeRunSync()
  }
  it should "properly remove table" in {
    val id = 4L
    val t  = Table(id = id.some, name = "test4", participants = 0)
    testWebsockets(
      msgsToSend =
        List(login("admin", "admin"), add_table(-1, t), remove_table(id)),
      expected = List(
        login_successful("admin"),
        table_added(-1, t),
        table_removed(id)
      )
    ).unsafeRunSync()
  }

  override protected def beforeAll(): Unit = {
    container.start()
    Starter.start().unsafeRunAsyncAndForget()
  }

  override protected def afterAll(): Unit =
    container.stop()
}
