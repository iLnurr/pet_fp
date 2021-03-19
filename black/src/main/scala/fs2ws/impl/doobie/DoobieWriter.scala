package fs2ws.impl.doobie

import cats.effect.{ Async, ContextShift }
import cats.syntax.functor._
import doobie.implicits._
import doobie.util.fragment.Fragment
import fs2ws.DbWriter
import fs2ws.Domain.{ DBEntity, Table, User }

abstract class DoobieWriter[F[_]: Async: ContextShift: DoobieService, T <: DBEntity] extends DbWriter[F, T] {
  def create(): Fragment
  def addSql(after_id: Long, ent: T): Fragment
  def updateSql(ent: T): Fragment
  def removeSql(id: Long): Fragment

  def createTables(): F[Either[Throwable, Int]] =
    DoobieService[F].upsert(create())

  override def add(after_id: Long, ent: T): F[Either[Throwable, T]] =
    DoobieService[F].upsert(addSql(after_id, ent)).map(_.map(_ => ent))
  override def update(ent: T): F[Either[Throwable, Unit]] =
    DoobieService[F].upsert(updateSql(ent)).map(_.map(_ => ()))
  override def remove(id: Long): F[Either[Throwable, Unit]] =
    DoobieService[F].upsert(removeSql(id)).map(_.map(_ => ()))
}

class UserWriter[F[_]: Async: ContextShift: DoobieService] extends DoobieWriter[F, User] {
  def create(): Fragment = sql"""
    CREATE TABLE IF NOT EXISTS users (
      uid   SERIAL,
      id BIGINT NOT NULL,
      name VARCHAR NOT NULL,
      password VARCHAR NOT NULL,
      user_type VARCHAR NOT NULL
    )
  """

  override def addSql(after_id: Long, ent: User): Fragment =
    sql"insert into users(id,name,password,user_type) values (${after_id + 1},${ent.name},${ent.password},${ent.user_type})"

  override def updateSql(ent: User): Fragment =
    sql"update users set name = ${ent.name}, password = ${ent.password}, user_type = ${ent.user_type} where id = ${ent.id}"

  override def removeSql(id: Long): Fragment =
    sql"delete from users where id=$id"
}
object UserWriter {
  def apply[F[_]](implicit inst: UserWriter[F]): UserWriter[F] = inst
}

class TableWriter[F[_]: Async: ContextShift: DoobieService] extends DoobieWriter[F, Table] {
  def create(): Fragment = sql"""
    CREATE TABLE IF NOT EXISTS tables (
      uid   SERIAL,
      id BIGINT NOT NULL,
      name VARCHAR NOT NULL,
      participants BIGINT NOT NULL
    )
  """
  override def addSql(after_id: Long, ent: Table): Fragment =
    sql"insert into tables(id,name,participants) values (${after_id + 1},${ent.name},${ent.participants})"

  override def updateSql(ent: Table): Fragment =
    sql"update tables set name = ${ent.name}, participants = ${ent.participants} where id = ${ent.id}"

  override def removeSql(id: Long): Fragment =
    sql"delete from tables where id=$id"
}
object TableWriter {
  def apply[F[_]](implicit inst: TableWriter[F]): TableWriter[F] = inst
}
