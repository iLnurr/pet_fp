package fs2ws.impl

import cats.effect.{Async, ContextShift}
import cats.syntax.functor._
import doobie.util.fragment.Fragment
import fs2ws.DbWriterAlgebra
import fs2ws.Domain.{DBEntity, Table}
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

abstract class DbWriterImpl[F[_]: Async: ContextShift, T <: DBEntity](
  db: DbImpl[F]
) extends DbWriterAlgebra[F, T] {
  def addSql(after_id: Long, ent: T): Fragment
  def updateSql(ent:   T): Fragment
  def removeSql(id:    Long): Fragment

  implicit val xa: Aux[F, Unit] = db.startTransactor()
  override def add(after_id: Long, ent: T): F[Either[Throwable, T]] =
    db.upsert(addSql(after_id, ent)).map(_.map(_ => ent))
  override def update(ent: T): F[Either[Throwable, Unit]] =
    db.upsert(updateSql(ent)).map(_.map(_ => ()))
  override def remove(id: Long): F[Either[Throwable, Unit]] =
    db.upsert(removeSql(id)).map(_.map(_ => ()))
}

class TableWriterImpl[F[_]: Async: ContextShift](
  db: DbImpl[F]
) extends DbWriterImpl[F, Table](db) {
  override def addSql(after_id: Long, ent: Table): Fragment =
    sql"insert into tables(id,name,participants) values (${after_id + 1},${ent.name},${ent.participants})"

  override def updateSql(ent: Table): Fragment =
    sql"update tables set name = ${ent.name} AND participants = ${ent.participants} where id = ${ent.id}"

  override def removeSql(id: Long): Fragment =
    sql"delete from tables where id=$id"
}
