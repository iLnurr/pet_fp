package fs2ws

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Timer}
import com.typesafe.scalalogging.Logger
import fs2ws.impl.State.ConnectedClients
import fs2ws.impl._

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] =
    Starter.start()

}

object Starter {
  private val logger = Logger("Main")
  def start()(
    implicit ce: ConcurrentEffect[IO],
    cs:          ContextShift[IO],
    t:           Timer[IO]
  ): IO[ExitCode] =
    (for {
      clients <- ConnectedClients.create[IO]
//      db          <- IO.delay(new DbImpl[IO])
//      userReader  <- IO.delay(new UserReaderImpl[IO](db))
//      userWriter  <- IO.delay(new UserWriterImpl[IO](db))
//      tableReader <- IO.delay(new TableReaderImpl[IO](db))
//      tableWriter <- IO.delay(new TableWriterImpl[IO](db))
      userReader  <- IO.delay(InMemoryDB.Users)
      userWriter  <- IO.delay(InMemoryDB.Users)
      tableReader <- IO.delay(InMemoryDB.Tables)
      tableWriter <- IO.delay(InMemoryDB.Tables)
      _ <- new ServerImpl(
        clients,
        Http4sWebsocketServer.start(_),
        new Services[IO](
          userReader  = userReader,
          tableReader = tableReader,
          tableWriter = tableWriter
        )
      ).start()
    } yield {
      ExitCode.Success
    }).handleErrorWith(
      ex =>
        IO {
          logger.info(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
    )
}
