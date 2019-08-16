package fs2ws

import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import fs2ws.Domain._

import scala.collection.mutable.ArrayBuffer

object Services {
  def handle: Message => IO[Message] = {
    case msg: AuthMsg => msg match {
      case ar: AuthReq =>
        auth(ar)
      case _ =>
        IO.raiseError(new RuntimeException(s"Can't handle $msg"))
    }
    case commands: PrivilegedCommands => commands match {
      case addTableReq: AddTableReq =>
        tables(addTableReq)
      case updateTableReq: UpdateTableReq =>
        tables(updateTableReq)
      case removeTableReq: RemoveTableReq =>
        tables(removeTableReq)
    }
    case msg: TableMsg =>
      tables(msg)
    case msg: PingMsg => msg match {
      case pr: PingReq =>
        ping(pr)
      case _ =>
        IO.raiseError(new RuntimeException(s"Can't handle $msg"))
    }
    case msg =>
      IO.raiseError(new RuntimeException(s"Can't handle $msg"))
  }

  private def auth: AuthReq => IO[Message] = ar =>
    Users.getByName(ar.username).map {
      case Some(user) =>
        AuthSuccessResp(user.user_type)
      case None =>
        AuthFailResp()
    }

  private def ping: PingReq => IO[Message] = req =>
    IO.pure(PongResponse(req.seq))

  private def tables: TableMsg => IO[TableMsg] = {
    case SubscribeTables(_) =>
      Tables.list.map(seq => TableList(seq))
    case AddTableReq(after_id, table, _) =>
      Tables.add(table).flatMap {
        case Left(value) =>
          IO.pure(UpdateTableFailResponse(after_id))
        case Right(inserted) =>
          IO.pure(UpdateTableResponse(inserted))
      }
    case UpdateTableReq(table, _) =>
      Tables.update(table).flatMap {
        case Left(value) =>
          IO.pure(UpdateTableFailResponse(table.id.getOrElse(-1L)))
        case Right(value) =>
          IO.pure(UpdateTableResponse(table))
      }
    case RemoveTableReq(id, _) =>
      Tables.remove(id).flatMap {
        case Left(value) =>
          IO.pure(RemoveTableFailResponse(id))
        case Right(value) =>
          IO.pure(RemoveTableResponse(id))
      }
  }
}
class DB[F[_],T <: DBEntity](implicit F: Sync[F]) {
  private val repo = ArrayBuffer[T]()
  def getById(id: Long): F[Option[T]] =
    F.delay(repo.find(_.id == Option(id)))
  def getByName(n: String): F[Option[T]] =
    F.delay(repo.find(_.name == n))
  def add(ent: T): F[Either[Throwable, T]] =
    F.delay(Right{
      repo.append(ent)
      ent
    })
  def list: F[Seq[T]] =
    F.delay(repo.toSeq)
  def update(ent: T): F[Either[Throwable, Unit]] =
    F.delay{
      repo.find(_.id == ent.id).map{e =>
        repo -= e
        e
      }
      repo += ent
      Right(())
    }

  def remove(id: Long): F[Either[Throwable, Unit]] =
    F.delay {
      repo.find(_.id == Option(id)).map(e => repo -= e)
      Right(())
    }
}

object Users extends DB[IO, User] {
  val admin = User(Option(0L), "admin", "admin", UserType.ADMIN)
  val user = User(Option(1L), "un", "upwd", UserType.USER)

  (add(admin) >> add(user)).unsafeRunSync()
}
object Tables extends DB[IO, Table]
