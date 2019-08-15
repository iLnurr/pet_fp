package fs2ws

object domain {

  sealed trait Message {
    def $type: String
  }

  sealed trait AuthMsg extends Message
  sealed trait PrivilegedCommands extends Message // Only admins are allowed to use these commands
  sealed trait TableMsg extends Message

  case class AuthReq(username: String, password: String) extends AuthMsg {
    val $type: String = "login"
  }
  case object AuthFailResp extends AuthMsg {
    val $type: String = "login_failed"
  }
  case class AuthSuccessResp(user_type: String) extends AuthMsg {
    val $type: String = "login_successful"
  }

  case object NotAuthorized extends PrivilegedCommands {
    val $type: String = "not_authorized"
  }

  sealed trait PingMsg extends Message {
    def seq: Long
  }
  case class PingReq(seq: Long) extends PingMsg {
    val $type: String = "ping"
  }
  case class PongResponse(seq: Long) extends PingMsg {
    val $type: String = "pong"
  }

  case class Table(id: Option[Long] = None, name: String, participants: Long)
  case object SubscribeTables extends TableMsg {
    val $type: String = "subscribe_tables"
  }
  case class TableList(tables: Seq[Table]) extends TableMsg {
    val $type: String = "table_list"
  }
  case object UnsubscribeTables extends TableMsg {
    val $type: String = "unsubscribe_tables"
  }

  case class AddTableReq(after_id: Long, table: Table) extends TableMsg with PrivilegedCommands {
    val $type: String = "add_table"
  }
  case class UpdateTableReq(table: Table) extends TableMsg with PrivilegedCommands {
    val $type: String = "update_table"
  }
  case class RemoveTableReq(id: Long) extends TableMsg with PrivilegedCommands {
    val $type: String = "remove_table"
  }
  case class UpdateTableFailResponse(id: Long) extends TableMsg with PrivilegedCommands {
    val $type: String = "update_failed"
  }
  case class RemoveTableFailResponse(id: Long) extends TableMsg with PrivilegedCommands {
    val $type: String = "removal_failed"
  }
  case class TableAddedResponse(after_id: Long, table: Table) extends TableMsg with PrivilegedCommands {
    val $type: String = "table_added"
  }
  case class RemoveTableResponse(id: Long) extends TableMsg with PrivilegedCommands {
    val $type: String = "table_removed"
  }
  case class UpdateTableResponse(table: Table) extends TableMsg with PrivilegedCommands {
    val $type: String = "table_updated"
  }

}
