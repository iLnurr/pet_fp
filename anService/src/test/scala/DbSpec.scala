import db.xa
import doobie.implicits._
import org.scalatest.{Assertion, FlatSpec, Matchers}

class DbSpec extends FlatSpec with Matchers {
  behavior.of("DOOBIE")

  it should "properly work script" in {
    check()
  }

  def check(): Assertion = {
    def populate(id2: Long, name: String, next: String) =
      sql"insert into db.test(id2,name,next) values ($id2,$name,$next)".update.run
        .transact(xa)

    val p6 = db.getRecords(
      fields = Seq("id", "id2", "name", "next"),
      kvEq   = Seq(),
      kvLess = Seq(),
      kvMore = Seq()
    )
    val p7 = db.getRecords(
      fields = Seq("id", "id2", "name", "next"),
      kvEq   = Seq("id" -> "1"),
      kvLess = Seq(),
      kvMore = Seq()
    )
    val p8 = db.getRecords(
      fields = Seq("id", "id2", "name", "next"),
      kvEq   = Seq("id" -> "1"),
      kvLess = Seq("id2" -> "5"),
      kvMore = Seq()
    )
    val p9 = db.getRecords(
      fields = Seq("id", "id2", "name", "next"),
      kvEq   = Seq("id" -> "1"),
      kvLess = Seq("id2" -> "5"),
      kvMore = Seq("id2" -> "-1")
    )
    val program = for {
      _ <- sql"create database if not exists db".update.run
        .transact(xa)
      _ <- sql"create table if not exists db.test (id bigint primary key auto_increment, id2 bigint, name text, next text)".update.run
        .transact(xa)
      _ <- populate(0, "n", "n")
      _ <- populate(1, "n1", "n1")
      _ <- populate(2, "n2", "n2")
      _ <- populate(3, "n3", "n3")
      _ <- populate(4, "n4", "n4")
      _ <- populate(5, "n5", "n5")
      p41 <- sql"select * from test"
        .query[(String, String)]
        .to[List]
        .transact(xa)
      p61 <- p6
      p71 <- p7
      p81 <- p8
      p91 <- p9
    } yield {
      val allSize = p41.size
      p61._2.size shouldBe allSize
      p71._2.size shouldBe 1
      p81._2.size shouldBe 1
      p91._2.size shouldBe 1
    }

    program.unsafeRunSync()
  }
}
