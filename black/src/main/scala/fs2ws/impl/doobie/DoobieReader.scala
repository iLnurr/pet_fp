package fs2ws.impl.doobie

import cats.effect.{Async, ContextShift}
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.query.Query0
import fs2ws.DbReader
import fs2ws.Domain.{DBEntity, Table, User}

abstract class DoobieReader[F[_]: Async: ContextShift: DoobieService, T <: DBEntity]
    extends DbReader[F, T] {
  def getByIdQuery(id:  Long):   Query0[T]
  def getByNameQuery(n: String): Query0[T]
  def listQuery: Query0[T]

  override def getById(id: Long): F[Option[T]] =
    DoobieService[F]
      .selectStream(getByIdQuery(id))
      .compile
      .toList
      .map(_.headOption)

  override def getByName(n: String): F[Option[T]] =
    DoobieService[F]
      .selectStream(getByNameQuery(n))
      .compile
      .toList
      .map(_.headOption)

  override def list: F[Seq[T]] =
    DoobieService[F].selectStream(listQuery).compile.toVector.map(_.toSeq)
}

class UserReader[F[_]: Async: ContextShift: DoobieService]
    extends DoobieReader[F, User] {
  override def getByIdQuery(id: Long): Query0[User] =
    sql"select id, name, password, user_type from users where id = $id"
      .query[User]

  override def getByNameQuery(n: String): Query0[User] =
    sql"select id, name, password, user_type from users where name = $n"
      .query[User]

  override def listQuery: Query0[User] =
    sql"select id, name, password, user_type from users".query[User]
}
object UserReader {
  def apply[F[_]](implicit inst: UserReader[F]): UserReader[F] = inst
}

class TableReader[F[_]: Async: ContextShift: DoobieService]
    extends DoobieReader[F, Table] {
  override def getByIdQuery(id: Long): Query0[Table] =
    sql"select id, name, participants from tables where id = $id".query[Table]

  override def getByNameQuery(n: String): Query0[Table] =
    sql"select id, name, participants from tables where name = $n".query[Table]

  override def listQuery: Query0[Table] =
    sql"select id, name, participants from tables".query[Table]
}
object TableReader {
  def apply[F[_]](implicit inst: TableReader[F]): TableReader[F] = inst
}
