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
      case ar: login =>
        auth(ar)
      case _ =>
        IO.raiseError(new RuntimeException(s"Can't handle $msg"))
    }
    case commands: PrivilegedCommands => commands match {
      case addTableReq: add_table =>
        tables(addTableReq)
      case updateTableReq: update_table =>
        tables(updateTableReq)
      case removeTableReq: remove_table =>
        tables(removeTableReq)
    }
    case msg: TableMsg =>
      tables(msg)
    case msg: PingMsg => msg match {
      case pr: ping =>
        pingF(pr)
      case _ =>
        IO.raiseError(new RuntimeException(s"Can't handle $msg"))
    }
    case msg =>
      IO.raiseError(new RuntimeException(s"Can't handle $msg"))
  }

  def tableList: IO[table_list] =
    Tables.list.map(seq => table_list(seq))

  private def auth: login => IO[Message] = ar =>
    Users.getByName(ar.username).map {
      case Some(user) if ar.password == user.password =>
        login_successful(user.user_type)
      case _ =>
        login_failed()
    }

  private def pingF: ping => IO[Message] = req =>
    IO.pure(pong(req.seq))

  private def tables: TableMsg => IO[Message] = {
    case subscribe_tables() =>
      tableList
    case _:unsubscribe_tables =>
      IO.pure(empty)
    case add_table(after_id, table) =>
      Tables.add(after_id, table).map {
        case Left(_) =>
          add_failed(after_id)
        case Right(inserted) =>
          table_added(after_id,inserted)
      }
    case update_table(table) =>
      Tables.update(table).map {
        case Left(_) =>
          update_failed(table.id.getOrElse(-1L))
        case Right(_) =>
          table_updated(table)
      }
    case remove_table(id) =>
      Tables.remove(id).map {
        case Left(_) =>
          removal_failed(id)
        case Right(_) =>
          table_removed(id)
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
