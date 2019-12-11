package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import com.typesafe.scalalogging.Logger
import fs2ws.impl.State.ConnectedClients
import fs2ws.impl._

object Main extends IOApp {
  private val logger = Logger("Main")
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      clients     <- ConnectedClients.create[IO]
      db          <- IO.delay(new DbImpl[IO])
      userReader  <- IO.delay(new UserReaderImpl[IO](db))
      tableReader <- IO.delay(new TableReaderImpl[IO](db))
      tableWriter <- IO.delay(new TableWriterImpl[IO](db))
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
