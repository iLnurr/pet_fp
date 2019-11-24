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
    case msg: AuthMsg =>
      msg match {
        case ar: login =>
          auth(ar)
        case _ =>
          Sync[F].raiseError(new RuntimeException(s"Can't handle $msg"))
      }
    case commands: PrivilegedCommands =>
      commands match {
        case addTableReq: add_table =>
          tables(addTableReq)
        case updateTableReq: update_table =>
          tables(updateTableReq)
        case removeTableReq: remove_table =>
          tables(removeTableReq)
      }
    case msg: TableMsg =>
      tables(msg)
    case msg: PingMsg =>
      msg match {
        case pr: ping =>
          pingF(pr)
        case _ =>
          Sync[F].raiseError(new RuntimeException(s"Can't handle $msg"))
      }
    case msg =>
      Sync[F].raiseError(new RuntimeException(s"Can't handle $msg"))
  }

  def tableList: F[Message] =
    tableReader.list.map(seq => table_list(seq))

  private def auth: login => F[Message] =
    ar =>
      userReader.getByName(ar.username).map {
        case Some(user) if ar.password == user.password =>
          login_successful(user.user_type)
        case _ =>
          login_failed()
      }

  private def pingF: ping => F[Message] = req => Sync[F].pure(pong(req.seq))

  private def tables: TableMsg => F[Message] = {
    case subscribe_tables() =>
      tableList
    case _: unsubscribe_tables =>
      Sync[F].pure(empty)
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
    case other =>
      Sync[F].raiseError(new RuntimeException(s"Bad request: $other"))
  }
}
