import cats.effect.IO
import db.{Data, Headers}
import doobie.Transactor
import doobie.implicits._
import model.GetInfo
import org.scalatest.{Assertion, FlatSpec, Matchers}

class DbSpec extends FlatSpec with Matchers {
  behavior.of("DOOBIE")

  it should "properly work script" in {
    check()
  }

  def check(): Assertion = {
    implicit val xa = db.startTransactor()
    def populate(
      id2:       Long,
      name:      String,
      title:     String,
      price:     Long,
      rooms:     Int,
      houseType: String,
      region:    String
    ) =
      sql"insert into db.test(id2,name,title,price,rooms,houseType,region) values ($id2,$name,$title,$price,$rooms,$houseType,$region)".update.run
        .transact(xa)

    def getRecords(
      tableName:   String,
      fields:      Seq[String],
      kvEq:        Seq[(String, String)],
      kvMore:      Seq[(String, String)],
      kvLess:      Seq[(String, String)]
    )(implicit xa: Transactor[IO]): IO[(Headers, Data)] =
      db.getRecords(
        db.constructQuery(
          GetInfo(tableName, fields, kvEq.toMap, kvMore.toMap, kvLess.toMap)
        )
      )

    val p6 = getRecords(
      tableName = "test",
      fields    = Seq("id", "id2", "name", "price"),
      kvEq      = Seq(),
      kvLess    = Seq(),
      kvMore    = Seq()
    )
    val p7 = getRecords(
      tableName = "test",
      fields    = Seq("id", "id2", "name", "price"),
      kvEq      = Seq("id" -> "1"),
      kvLess    = Seq(),
      kvMore    = Seq()
    )
    val p8 = getRecords(
      tableName = "test",
      fields    = Seq("id", "id2", "name", "price"),
      kvEq      = Seq("id" -> "1"),
      kvLess    = Seq("id2" -> "5"),
      kvMore    = Seq()
    )
    val p9 = getRecords(
      tableName = "test",
      fields    = Seq("id", "id2", "name", "price"),
      kvEq      = Seq("id" -> "1"),
      kvLess    = Seq("id2" -> "5"),
      kvMore    = Seq("id2" -> "-1")
    )
    val program = for {
      _ <- sql"create database if not exists db".update.run
        .transact(xa)
      _ <- sql"create table if not exists db.test (id bigint primary key auto_increment, id2 bigint, name text, title text, price bigint, rooms bigint, houseType text, region text)".update.run
        .transact(xa)
      _ <- populate(0, "n", "n", 10, 2, "flats", "BAR")
      _ <- populate(1, "n1", "n1", 100, 8, "apartment", "BAR")
      _ <- populate(2, "n2", "n2", 1000, 10, "village", "BAR")
      _ <- populate(3, "n3", "n3", 10000, 16, "apartment", "BAR")
      _ <- populate(4, "n4", "n4", 100000, 22, "flats", "BAR")
      _ <- populate(5, "n5", "n5", 1000000, 24, "village", "BAR")
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
