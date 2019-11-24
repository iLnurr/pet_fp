import cats.effect.IO
import db.{dbLogger, xa}
import org.scalatest.{FlatSpec, Matchers}
import doobie.implicits._

class DbSpec extends FlatSpec with Matchers {
  behavior.of("DOOBIE")

  it should "properly work script" in {
    check.unsafeRunSync()
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
    val p6 = db.getRecords(
      fields = Seq("id", "id2", "name", "next"),
      kvEq   = Seq("id" -> "0"),
      kvLess = Seq("id2" -> "5"),
      kvMore = Seq("id2" -> "0")
    )
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
      p61 <- p6
    } yield {
      dbLogger.debug(s"p11 $p11")
      dbLogger.debug(s"p21 $p21")
      dbLogger.debug(s"p31 $p31")
      dbLogger.debug(s"p41 \n${p41.mkString("\n")}")
      dbLogger.debug(s"p51 $p51")
      dbLogger.debug(s"p61 $p61")
      p11
    }
  }
}
