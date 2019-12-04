package fs2ws

import cats.effect.Sync
import cats.syntax.functor._
import fs2ws.Domain._

class Services[F[_]: Sync](
  userReader:  UserReader[F],
  tableReader: TableReader[F],
  tableWriter: TableWriter[F]
) {
  def handleReq: Message => F[Message] = {
    case command: Command =>
      processCommand(command)
    case query: Query =>
      processQuery(query)
    case msg =>
      Sync[F].raiseError(new RuntimeException(s"Can't handle $msg")) // TODO avoid exceptions
  }

  def tableList: F[Message] =
    tableReader.list.map(seq => table_list(seq))

  private def processQuery: Query => F[Message] = {
    case login(username, password) =>
      userReader.getByName(username).map {
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
      tableWriter.add(after_id, table).map {
        case Left(_) =>
          add_failed(after_id)
        case Right(inserted) =>
          table_added(after_id, inserted)
      }
    case update_table(table) =>
      tableWriter.update(table).map {
        case Left(_) =>
          update_failed(table.id.getOrElse(-1L))
        case Right(_) =>
          table_updated(table)
      }
    case remove_table(id) =>
      tableWriter.remove(id).map {
        case Left(_) =>
          removal_failed(id)
        case Right(_) =>
          table_removed(id)
      }
  }
}
