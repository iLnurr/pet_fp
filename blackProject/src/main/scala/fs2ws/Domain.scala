package fs2ws

object Domain {
  object MsgTypes {
    val LOGIN             = "login"
    val ADD_TABLE         = "add_table"
    val UPDATE_TABLE      = "update_table"
    val REMOVE_TABLE      = "remove_table"
    val PING              = "ping"
    val SUBSCRIBE_TABLE   = "subscribe_tables"
    val UNSUBSCRIBE_TABLE = "unsubscribe_tables"

    val PONG             = "pong"
    val LOGIN_FAILED     = "login_failed"
    val LOGIN_SUCCESSFUL = "login_successful"
    val NOT_AUTHORIZED   = "not_authorized"
    val TABLE_LIST       = "table_list"
    val TABLE_ADDED      = "table_added"
    val TABLE_UPDATED    = "table_updated"
    val UPDATE_FAILED    = "update_failed"
    val REMOVAL_FAILED   = "removal_failed"
    val TABLE_REMOVED    = "table_removed"
  }

  sealed trait Message
  case object empty extends Message

  sealed trait AuthMsg extends Message
  sealed trait PrivilegedCommands extends Message // Only admins are allowed to use these commands
  sealed trait TableMsg extends Message
  sealed trait PingMsg extends Message {
    def seq: Long
  }
  case class not_authorized() extends Message

  case class login(username: String, password: String) extends AuthMsg
  case class login_failed() extends AuthMsg
  case class login_successful(user_type: String) extends AuthMsg

  case class add_table(after_id: Long, table: Table)
      extends TableMsg
      with PrivilegedCommands
  case class update_table(table: Table) extends TableMsg with PrivilegedCommands
  case class remove_table(id:    Long) extends TableMsg with PrivilegedCommands

  case class subscribe_tables() extends TableMsg
  case class table_list(tables: Seq[Table]) extends TableMsg
  case class unsubscribe_tables() extends TableMsg

  case class update_failed(id:     Long) extends TableMsg
  case class removal_failed(id:    Long) extends TableMsg
  case class table_added(after_id: Long, table: Table) extends TableMsg
  case class add_failed(after_id:  Long) extends TableMsg
  case class table_removed(id:     Long) extends TableMsg
  case class table_updated(table:  Table) extends TableMsg

  case class ping(seq: Long) extends PingMsg
  case class pong(seq: Long) extends PingMsg

  sealed trait DBEntity {
    def id:   Option[Long]
    def name: String
  }
  case class Table(id: Option[Long] = None, name: String, participants: Long)
      extends DBEntity
  case class User(
    id:        Option[Long] = None,
    name:      String,
    password:  String,
    user_type: String
  ) extends DBEntity
  object UserType {
    val ADMIN = "admin"
    val USER  = "user"
  }
}
