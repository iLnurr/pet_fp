package fs2ws

import java.util.concurrent.atomic.AtomicLong

import cats.effect.{IO, Sync}
import cats.syntax.flatMap._
import fs2ws.Domain._

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object Services {
  def handleReq: Message => IO[Message] = {
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

  def tableList: IO[TableList] =
    Tables.list.map(seq => TableList(seq))

  private def auth: AuthReq => IO[Message] = ar =>
    Users.getByName(ar.username).map {
      case Some(user) if ar.password == user.password =>
        AuthSuccessResp(user.user_type)
      case _ =>
        AuthFailResp()
    }

  private def ping: PingReq => IO[Message] = req =>
    IO.pure(PongResponse(req.seq))

  private def tables: TableMsg => IO[Message] = {
    case SubscribeTables(_) =>
      tableList
    case _:UnsubscribeTables =>
      IO.pure(EmptyMsg)
    case AddTableReq(after_id, table, _) =>
      Tables.add(after_id, table).flatMap {
        case Left(_) =>
          IO.pure(AddTableFailResponse(after_id))
        case Right(inserted) =>
          IO.pure(AddTableResponse(after_id,inserted))
      }
    case UpdateTableReq(table, _) =>
      Tables.update(table).flatMap {
        case Left(_) =>
          IO.pure(UpdateTableFailResponse(table.id.getOrElse(-1L)))
        case Right(_) =>
          IO.pure(UpdateTableResponse(table))
      }
    case RemoveTableReq(id, _) =>
      Tables.remove(id).flatMap {
        case Left(_) =>
          IO.pure(RemoveTableFailResponse(id))
        case Right(_) =>
          IO.pure(RemoveTableResponse(id))
      }
    case other =>
      IO.raiseError(new RuntimeException(s"Bad request: $other"))
  }
}
abstract class DB[F[_],T <: DBEntity](implicit F: Sync[F]) {
  private val repo = ArrayBuffer[T]()
  private val counter = new AtomicLong(0L)
  def setIdIfEmpty: Long => T => T
  def getById(id: Long): F[Option[T]] =
    F.delay(repo.find(_.id == Option(id)))
  def getByName(n: String): F[Option[T]] =
    F.delay(repo.find(_.name == n))
  def add(after_id: Long, ent: T): F[Either[Throwable, T]] =
    F.delay(Try{
      val checked = setIdIfEmpty(counter.incrementAndGet())(ent)
      if (after_id < 0) {
        repo.prepend(checked)
      } else if (repo.size < after_id) {
        repo.append(checked)
      } else {
        repo.insert((after_id + 1L).toInt, checked)
      }
      println(repo)
      checked
    }.toEither)
  def list: F[Seq[T]] =
    F.delay(repo.toSeq)
  def update(ent: T): F[Either[Throwable, Unit]] =
    F.delay{
      Try{
        repo
          .find(_.id == ent.id)
          .foreach(e => repo -= e)
        repo.append(ent)
      }.toEither
    }

  def remove(id: Long): F[Either[Throwable, Unit]] =
    F.delay {
      Try(repo.find(_.id == Option(id))
        .foreach(e => repo -= e))
        .toEither
    }
}

object Users extends DB[IO, User] {
  val admin = User(Option(0L), "admin", "admin", UserType.ADMIN)
  val user = User(Option(1L), "un", "upwd", UserType.USER)

  (add(-1, admin) >> add(0,user)).unsafeRunSync()

  override def setIdIfEmpty: Long => User => User = newId => input =>
    input.copy(id = Some(input.id.getOrElse(newId)))
}
object Tables extends DB[IO, Table] {
  override def setIdIfEmpty: Long => Table => Table = newId => input =>
    input.copy(id = Some(input.id.getOrElse(newId)))
}
