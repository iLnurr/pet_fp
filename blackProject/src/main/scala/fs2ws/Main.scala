package fs2ws

import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import fs2ws.impl.JsonSerDe._
import fs2ws.impl.{InMemoryDB, ServerImpl}
import fs2ws.impl.State.ConnectedClients

object Main extends IOApp {
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  val userReader:  UserReader[IO]       = InMemoryDB.Users
  val tableReader: TableReader[IO]      = InMemoryDB.Tables
  val tableWriter: TableWriter[IO]      = InMemoryDB.Tables
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      clients <- ConnectedClients.create[IO]
      _ <- new ServerImpl(
        clients,
        startMsgStream(encoder.toJson, decoder.fromJson, _),
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
          println(s"Server stopped with error ${ex.getLocalizedMessage}")
          ExitCode.Error
        }
    )
}
