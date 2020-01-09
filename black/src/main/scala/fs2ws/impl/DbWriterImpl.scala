package fs2ws.impl

import cats.effect.{Async, ContextShift}
import cats.syntax.functor._
import doobie.util.fragment.Fragment
import fs2ws.DbWriter
import fs2ws.Domain.{DBEntity, Table, User}
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

abstract class DbWriterImpl[F[_]: Async: ContextShift, T <: DBEntity](
  db: DbImpl[F]
) extends DbWriter[F, T] {
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

class UserWriterImpl[F[_]: Async: ContextShift](
  db: DbImpl[F]
) extends DbWriterImpl[F, User](db) {
  override def addSql(after_id: Long, ent: User): Fragment =
    sql"insert into users(id,name,password,user_type) values (${after_id + 1},${ent.name},${ent.password},${ent.user_type})"

  override def updateSql(ent: User): Fragment =
    sql"update users set name = ${ent.name} AND password = ${ent.password} AND user_type = ${ent.user_type} where id = ${ent.id}"

  override def removeSql(id: Long): Fragment =
    sql"delete from users where id=$id"
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
