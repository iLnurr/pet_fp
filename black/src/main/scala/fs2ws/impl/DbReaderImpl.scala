package fs2ws.impl

import cats.effect.{Async, ContextShift}
import cats.syntax.functor._
import doobie.util.query.Query0
import fs2ws.DbReader
import fs2ws.Domain.{DBEntity, Table, User}
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

abstract class DbReaderImpl[F[_]: Async: ContextShift, T <: DBEntity](
  db: DbImpl[F]
) extends DbReader[F, T] {
  def getByIdQuery(id:  Long):   Query0[T]
  def getByNameQuery(n: String): Query0[T]
  def listQuery: Query0[T]

  implicit val xa: Aux[F, Unit] = db.startTransactor()
  override def getById(id: Long): F[Option[T]] =
    db.selectStream(getByIdQuery(id)).compile.toList.map(_.headOption)

  override def getByName(n: String): F[Option[T]] =
    db.selectStream(getByNameQuery(n)).compile.toList.map(_.headOption)

  override def list: F[Seq[T]] =
    db.selectStream(listQuery).compile.toVector.map(_.toSeq)
}

class UserReaderImpl[F[_]: Async: ContextShift](
  db: DbImpl[F]
) extends DbReaderImpl[F, User](db) {
  override def getByIdQuery(id: Long): Query0[User] =
    sql"select * from users where id = $id".query[User]

  override def getByNameQuery(n: String): Query0[User] =
    sql"select * from users where name = $n".query[User]

  override def listQuery: Query0[User] = sql"select * from users".query[User]
}

class TableReaderImpl[F[_]: Async: ContextShift](
  db: DbImpl[F]
) extends DbReaderImpl[F, Table](db) {
  override def getByIdQuery(id: Long): Query0[Table] =
    sql"select * from tables where id = $id".query[Table]

  override def getByNameQuery(n: String): Query0[Table] =
    sql"select * from tables where name = $n".query[Table]

  override def listQuery: Query0[Table] = sql"select * from tables".query[Table]
}
