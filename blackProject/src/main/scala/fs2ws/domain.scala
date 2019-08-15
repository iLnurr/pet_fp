package fs2ws
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

object domain {

  sealed trait Message {
    def $type: String
  }

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

  sealed trait IncomingMsg extends Message
  sealed trait OutputMsg extends Message

  sealed trait AuthMsg extends Message
  sealed trait PrivilegedCommands extends Message // Only admins are allowed to use these commands
  sealed trait TableMsg extends Message

  case class AuthReq(username: String, password: String, $type: String = "login") extends AuthMsg with IncomingMsg
  case class AuthFailResp($type: String = "login_failed") extends AuthMsg with OutputMsg
  case class AuthSuccessResp(user_type: String, $type: String = "login_successful") extends AuthMsg with OutputMsg

  case class NotAuthorized($type: String = "not_authorized") extends PrivilegedCommands with OutputMsg

  sealed trait PingMsg extends Message {
    def seq: Long
  }
  case class PingReq(seq: Long, $type: String = "ping") extends PingMsg with IncomingMsg
  case class PongResponse(seq: Long, $type: String = "pong") extends PingMsg with OutputMsg

  case class SubscribeTables($type: String = "subscribe_tables") extends TableMsg with IncomingMsg
  case class TableList(tables: Seq[Table], $type: String = "table_list") extends TableMsg with OutputMsg
  case class UnsubscribeTables($type: String = "unsubscribe_tables") extends TableMsg with IncomingMsg

  case class AddTableReq(after_id: Long, table: Table, $type: String = "add_table") extends TableMsg with PrivilegedCommands with IncomingMsg
  case class UpdateTableReq(table: Table, $type: String = "update_table") extends TableMsg with PrivilegedCommands with IncomingMsg
  case class RemoveTableReq(id: Long, $type: String = "remove_table") extends TableMsg with PrivilegedCommands with IncomingMsg
  case class UpdateTableFailResponse(id: Long, $type: String = "update_failed") extends TableMsg with PrivilegedCommands with OutputMsg
  case class RemoveTableFailResponse(id: Long, $type: String = "removal_failed") extends TableMsg with PrivilegedCommands with OutputMsg
  case class TableAddedResponse(after_id: Long, table: Table, $type: String = "table_added") extends TableMsg with PrivilegedCommands with OutputMsg
  case class RemoveTableResponse(id: Long, $type: String = "table_removed") extends TableMsg with PrivilegedCommands with OutputMsg
  case class UpdateTableResponse(table: Table, $type: String = "table_updated") extends TableMsg with PrivilegedCommands with OutputMsg

}
