import cats.effect.IO
import com.dimafeng.testcontainers.MySQLContainer
import an.db.{Data, Headers}
import doobie.Transactor
import doobie.implicits._
import an.model.GetInfo
import org.scalatest.{Assertion, BeforeAndAfterAll, FlatSpec, Matchers}
import doobie.util.fragment.Fragment
import an.custom.queries._
import an.db

class DbSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior.of("DOOBIE")

  val mysql = new MySQLContainer(databaseName = Some("tradegoria"))
  override protected def beforeAll(): Unit =
    mysql.start()
  override protected def afterAll(): Unit =
    mysql.stop()

  implicit lazy val xa = db.startTransactor(
    url  = mysql.jdbcUrl,
    user = mysql.username,
    pass = mysql.password
  )
  it should "properly work with test db" in {
    check(xa)
  }

  it should "properly work with trad db" in {
    checkTrad(xa)
  }

  def checkTrad(implicit xa: Transactor[IO]): Assertion = {
    val program = for {
      _ <- Fragment
        .const(queries.createProducts)
        .update
        .run
        .transact(xa)
      _ <- Fragment
        .const(queries.createSiteContent)
        .update
        .run
        .transact(xa)
      products <- sql"select * from tradegoria.modx_ms2_products"
        .query[(String, String)]
        .to[List]
        .transact(xa)
    } yield {
      products.size shouldBe 0
    }

    program.unsafeRunSync()
  }
  def check(implicit xa: Transactor[IO]): Assertion = {
    def populate(
      id2:       Long,
      name:      String,
      title:     String,
      price:     Long,
      rooms:     Int,
      houseType: String,
      region:    String
    ) =
      sql"insert into test(id2,name,title,price,rooms,houseType,region) values ($id2,$name,$title,$price,$rooms,$houseType,$region)".update.run
        .transact(xa)

    def getRecords(
      tableName:   String,
      fields:      Seq[String],
      kvEq:        Seq[(String, String)],
      kvMore:      Seq[(String, String)],
      kvLess:      Seq[(String, String)]
    )(implicit xa: Transactor[IO]): IO[(Headers, Data)] =
      db.getRecords(
        constructQuery(
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
      _ <- sql"create table if not exists test (id bigint primary key auto_increment, id2 bigint, name text, title text, price bigint, rooms bigint, houseType text, region text)".update.run
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
