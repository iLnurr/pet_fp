import cats.effect.IO
import doobie._
import doobie.implicits._
import conf._
import com.typesafe.scalalogging.Logger
import shapeless._

object db {
  val dbLogger    = Logger("db")
  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  val xa = Transactor.fromDriverManager[IO](
    driver = dbDriver,
    url    = dbUrl,
    user   = dbUser,
    pass   = dbPass
  )

  def getRecords(
    fields: Seq[String],
    kvEq:   Seq[(String, String)],
    kvMore: Seq[(String, String)],
    kvLess: Seq[(String, String)],
    limit:  Long = 1000
  ) = {
    val tableName = "test"
    val kvEqQ =
      s"${kvEq.map { case (k, v) => k + "=" + v }.mkString("\n AND ")}"
    val kvMoreQ =
      s"${kvMore.map { case (k, v) => k + ">" + v }.mkString("\n AND ")}"
    val kvLessQ =
      s"${kvLess.map { case (k, v) => k + "<" + v }.mkString("\n AND ")}"

    val whereFragment = s"where \n$kvEqQ \n AND $kvMoreQ \n AND $kvLessQ"

    val result =
      sql"select ${fields.mkString(",")} from $tableName $whereFragment"

    // TODO types tuple

    result
      .query[(String, String)]
      .stream
      .take(limit)
      .compile
      .toList
      .transact(xa)
  }

  def check: IO[Int] = {
    val database = sql"create database if not exists db".update
    val table =
      sql"create table if not exists db.test (id bigint primary key auto_increment, id2 bigint, name text, next text)".update
    def populate(id2: Long, name: String, next: String) =
      sql"insert into db.test(id2,name,next) values ($id2,$name,$next)".update
    val all = sql"select * from test".query[(String, String)]
    val q   = sql"select 42".query[Int].unique

    val p1 = database.run.transact(xa)
    val p2 = table.run.transact(xa)
    val p3 = populate(0, "n", "n").run.transact(xa)
    val p4 = all.to[List].transact(xa)
    val p5 = q.transact(xa)
    for {
      p11 <- p1
      p21 <- p2
      p31 <- p3
      _   <- populate(1, "n1", "n1").run.transact(xa)
      _   <- populate(2, "n2", "n2").run.transact(xa)
      _   <- populate(3, "n3", "n3").run.transact(xa)
      _   <- populate(4, "n4", "n4").run.transact(xa)
      _   <- populate(5, "n5", "n5").run.transact(xa)
      p41 <- p4
      p51 <- p5
    } yield {
      dbLogger.debug(s"p11 $p11")
      dbLogger.debug(s"p21 $p21")
      dbLogger.debug(s"p31 $p31")
      dbLogger.debug(s"p41 \n${p41.mkString("\n")}")
      dbLogger.debug(s"p51 $p51")
      p11
    }
  }
}
