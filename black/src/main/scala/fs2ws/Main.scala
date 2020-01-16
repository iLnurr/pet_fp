package fs2ws

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import com.typesafe.scalalogging.Logger
import fs2ws.Domain.{User, UserType}
import fs2ws.impl.State.ConnectedClients
import fs2ws.impl._
import cats.syntax.all._
import fs2ws.impl.doobie.{
  DoobieService,
  TableReader,
  TableWriter,
  UserReader,
  UserWriter
}

object Main extends IOApp {
  implicit val ce:   ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit val conf: Conf[IO]             = new ConfImpl
  implicit val clients: Clients[IO] =
    ConnectedClients.create[IO].unsafeRunSync()
  implicit val db = new DoobieService[IO]
  implicit val userReader:     UserReader[IO]     = new UserReader[IO]
  implicit val userWriter:     UserWriter[IO]     = new UserWriter[IO]
  implicit val tableReader:    TableReader[IO]    = new TableReader[IO]
  implicit val tableWriter:    TableWriter[IO]    = new TableWriter[IO]
  implicit val messageService: MessageService[IO] = new MessageServiceImpl[IO]
  override def run(args: List[String]): IO[ExitCode] =
    Starter.start()

}

object Starter {
  private val logger = Logger("Main")
  def start[F[_]: ConcurrentEffect: ContextShift: Timer: Conf: Clients: DoobieService: UserReader: UserWriter: TableReader: TableWriter: MessageService]()
    : F[ExitCode] =
    (for {
      _ <- UserWriter[F].createTables()
      _ <- TableWriter[F].createTables()
      _ <- UserWriter[F].add(
        -1,
        User(Option(0L), "admin", "admin", UserType.ADMIN)
      )
      _ <- UserWriter[F].add(0, User(Option(1L), "un", "upwd", UserType.USER))
      _ <- new ServerImpl[F](
        Http4sWebsocketServer.start(_)
      ).start()
    } yield {
      ExitCode.Success
    }).handleErrorWith(
      ex => {
        logger.info(s"Server stopped with error ${ex.getLocalizedMessage}")
        ExitCode.Error
      }.pure[F]
    )
}
