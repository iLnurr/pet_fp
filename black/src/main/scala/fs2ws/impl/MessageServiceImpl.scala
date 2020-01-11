package fs2ws.impl

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import fs2ws.Domain._
import fs2ws.MessageService
import fs2ws.impl.doobie.{TableReader, TableWriter, UserReader}

class MessageServiceImpl[F[_]: Sync: UserReader: TableReader: TableWriter]
    extends MessageService[F] {
  def process: Message => F[Either[String, Message]] = {
    case command: Command =>
      processCommand(command).map(Right(_))
    case query: Query =>
      processQuery(query).map(Right(_))
    case msg =>
      s"Can't handle $msg".asLeft[Message].pure[F]
  }

  def tableList: F[Message] =
    TableReader[F].list.map(seq => table_list(seq))

  private def processQuery: Query => F[Message] = {
    case login(username, password) =>
      UserReader[F].getByName(username).map {
        case Some(user) if password == user.password =>
          login_successful(user.user_type)
        case _ =>
          login_failed()
      }
    case subscribe_tables() =>
      tableList
    case unsubscribe_tables() =>
      Sync[F].pure(empty)
    case ping(seq) =>
      Sync[F].pure(pong(seq))
  }

  private def processCommand: Command => F[Message] = {
    case add_table(after_id, table) =>
      TableWriter[F].add(after_id, table).map {
        case Left(_) =>
          add_failed(after_id)
        case Right(inserted) =>
          table_added(after_id, inserted)
      }
    case update_table(table) =>
      TableWriter[F].update(table).map {
        case Left(_) =>
          update_failed(table.id.getOrElse(-1L))
        case Right(_) =>
          table_updated(table)
      }
    case remove_table(id) =>
      TableWriter[F].remove(id).map {
        case Left(_) =>
          removal_failed(id)
        case Right(_) =>
          table_removed(id)
      }
  }
}
