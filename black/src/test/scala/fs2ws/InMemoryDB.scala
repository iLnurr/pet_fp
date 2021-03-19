package fs2ws.impl

import java.util.concurrent.atomic.AtomicLong

import cats.effect.{ IO, Sync }
import cats.syntax.flatMap._
import fs2ws.{ DbReader, DbWriter }
import fs2ws.Domain._

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object InMemoryDB {
  abstract class DbInMemory[F[_]: Sync, T <: DBEntity](buffer: ArrayBuffer[T])
      extends DbReader[F, T]
      with DbWriter[F, T] {
    private val counter = new AtomicLong(0L)
    def setIdIfEmpty: Long => T => T
    def getById(id: Long): F[Option[T]] =
      Sync[F].delay(buffer.find(_.id == Option(id)))
    def getByName(n: String): F[Option[T]] =
      Sync[F].delay(buffer.find(_.name == n))
    def add(after_id: Long, ent: T): F[Either[Throwable, T]] =
      Sync[F].delay(
        Try {
          val checked = setIdIfEmpty(counter.incrementAndGet())(ent)
          if (after_id < 0) {
            buffer.prepend(checked)
          } else if (buffer.size < after_id) {
            buffer.append(checked)
          } else {
            buffer.insert((after_id + 1L).toInt, checked)
          }
          println(buffer)
          checked
        }.toEither
      )
    def list: F[Seq[T]] =
      Sync[F].delay(buffer.toSeq)
    def update(ent: T): F[Either[Throwable, Unit]] =
      Sync[F].delay {
        Try {
          buffer
            .find(_.id == ent.id)
            .foreach(e => buffer -= e)
          buffer.append(ent)
          ()
        }.toEither
      }

    def remove(id: Long): F[Either[Throwable, Unit]] =
      Sync[F].delay {
        Try(
          buffer
            .find(_.id == Option(id))
            .foreach(e => buffer -= e)
        ).toEither
      }
  }

  private val usersRepo = new ArrayBuffer[User]()
  object Users extends DbInMemory[IO, User](usersRepo) {
    val admin = User(Option(0L), "admin", "admin", UserType.ADMIN)
    val user  = User(Option(1L), "un", "upwd", UserType.USER)

    (add(-1, admin) >> add(0, user)).unsafeRunSync()

    override def setIdIfEmpty: Long => User => User =
      newId => input => input.copy(id = Some(input.id.getOrElse(newId)))

    override def createTables(): IO[Either[Throwable, Int]] = IO.pure(Right(1))
  }
  private val tablesRepo = new ArrayBuffer[Table]()
  object Tables extends DbInMemory[IO, Table](tablesRepo) {
    override def setIdIfEmpty: Long => Table => Table =
      newId => input => input.copy(id = Some(input.id.getOrElse(newId)))

    override def createTables(): IO[Either[Throwable, Int]] = IO.pure(Right(1))
  }
}
