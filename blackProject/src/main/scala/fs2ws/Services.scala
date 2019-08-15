package fs2ws

import cats.Monad
import cats.effect.IO
import fs2ws.domain._

import scala.collection.mutable.ArrayBuffer

object Services {
  def auth: AuthReq => IO[OutputMsg] = ar =>
    Users.getByName(ar.username).map {
      case Some(user) =>
        AuthSuccessResp(user.user_type)
      case None =>
        AuthFailResp()
    }

  def ping: PingReq => IO[OutputMsg] = req =>
    IO.pure(PongResponse(req.seq))

  def tables: TableMsg => IO[OutputMsg] = {
    case domain.SubscribeTables(_) =>
      Tables.list.map(seq => TableList(seq))
    case domain.AddTableReq(after_id, table, _) =>
      Tables.add(table).flatMap {
        case Left(value) =>
          IO.pure(UpdateTableFailResponse(after_id))
        case Right(inserted) =>
          IO.pure(UpdateTableResponse(inserted))
      }
    case domain.UpdateTableReq(table, _) =>
      Tables.update(table).flatMap {
        case Left(value) =>
          IO.pure(UpdateTableFailResponse(table.id.getOrElse(-1L)))
        case Right(value) =>
          IO.pure(UpdateTableResponse(table))
      }
    case domain.RemoveTableReq(id, _) =>
      Tables.remove(id).flatMap {
        case Left(value) =>
          IO.pure(RemoveTableFailResponse(id))
        case Right(value) =>
          IO.pure(RemoveTableResponse(id))
      }
  }
}
class DB[F[_],T <: DBEntity](implicit F: Monad[F]) {
  private val repo = ArrayBuffer[T]()
  def getById(id: Long): F[Option[T]] =
    F.pure(repo.find(_.id == Option(id)))
  def getByName(n: String): F[Option[T]] =
    F.pure(repo.find(_.name == n))
  def add(ent: T): F[Either[Throwable, T]] =
    F.pure(Right{
      repo.append(ent)
      ent
    })
  def list: F[Seq[T]] =
    F.pure(repo.toSeq)
  def update(ent: T): F[Either[Throwable, Unit]] =
    F.pure{
      repo.find(_.id == ent.id).map{e =>
        repo -= e
        e
      }
      repo += ent
      Right(())
    }

  def remove(id: Long): F[Either[Throwable, Unit]] =
    F.pure {
      repo.find(_.id == Option(id)).map(e => repo -= e)
      Right(())
    }
}

object Users extends DB[IO, User] {
  val admin = User(Option(0L), "admin", "admin", UserType.ADMIN)
  val user = User(Option(1L), "un", "upwd", UserType.USER)

  add(admin)
  add(user)
}
object Tables extends DB[IO, Table]
