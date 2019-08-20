package fs2ws

import io.circe.generic.extras.Configuration


object Domain {
  object MsgTypes {
    val LOGIN = "login"
    val ADD_TABLE = "add_table"
    val UPDATE_TABLE = "update_table"
    val REMOVE_TABLE = "remove_table"
    val PING = "ping"
    val SUBSCRIBE_TABLE = "subscribe_tables"
    val UNSUBSCRIBE_TABLE = "unsubscribe_tables"

    val PONG = "pong"
    val LOGIN_FAILED = "login_failed"
    val LOGIN_SUCCESSFUL = "login_successful"
    val NOT_AUTHORIZED = "not_authorized"
    val TABLE_LIST = "table_list"
    val TABLE_ADDED = "table_added"
    val TABLE_UPDATED = "table_updated"
    val UPDATE_FAILED = "update_failed"
    val REMOVAL_FAILED = "removal_failed"
    val TABLE_REMOVED = "table_removed"
  }
  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("type")
  sealed trait Message {
    def $type: String
  }

  sealed trait AuthMsg extends Message
  sealed trait PrivilegedCommands extends Message // Only admins are allowed to use these commands
  sealed trait TableMsg extends Message
  sealed trait PingMsg extends Message {
    def seq: Long
  }
  case class NotAuthorized($type: String = "not_authorized") extends Message

  case class AuthReq(username: String, password: String, $type: String = "login") extends AuthMsg
  case class AuthFailResp($type: String = "login_failed") extends AuthMsg
  case class AuthSuccessResp(user_type: String, $type: String = "login_successful") extends AuthMsg

  case class AddTableReq(after_id: Long, table: Table, $type: String = "add_table") extends TableMsg with PrivilegedCommands
  case class UpdateTableReq(table: Table, $type: String = "update_table") extends TableMsg with PrivilegedCommands
  case class RemoveTableReq(id: Long, $type: String = "remove_table") extends TableMsg with PrivilegedCommands

  case class SubscribeTables($type: String = "subscribe_tables") extends TableMsg
  case class TableList(tables: Seq[Table], $type: String = "table_list") extends TableMsg
  case class UnsubscribeTables($type: String = "unsubscribe_tables") extends TableMsg

  case class UpdateTableFailResponse(id: Long, $type: String = "update_failed") extends TableMsg
  case class RemoveTableFailResponse(id: Long, $type: String = "removal_failed") extends TableMsg
  case class AddTableResponse(after_id: Long, table: Table, $type: String = "table_added") extends TableMsg
  case class AddTableFailResponse(after_id: Long, $type: String = "add_failed") extends TableMsg
  case class RemoveTableResponse(id: Long, $type: String = "table_removed") extends TableMsg
  case class UpdateTableResponse(table: Table, $type: String = "table_updated") extends TableMsg

  case class PingReq(seq: Long, $type: String = "ping") extends PingMsg
  case class PongResponse(seq: Long, $type: String = "pong") extends PingMsg


  sealed trait DBEntity {
    def id: Option[Long]
    def name: String
  }
  case class Table(id: Option[Long] = None, name: String, participants: Long) extends DBEntity
  case class User(id: Option[Long] = None, name: String, password: String, user_type: String) extends DBEntity
  object UserType {
    val ADMIN = "admin"
    val USER = "user"
  }

  case class Msg($type: String) extends Message
}
