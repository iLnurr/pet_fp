import cats.effect.IO
import doobie._
import doobie.implicits._
import conf._
import com.typesafe.scalalogging.Logger
import cats.implicits._

object db {
  // https://github.com/tpolecat/doobie/blob/master/modules/example/src/main/scala/example/Dynamic.scala
  type Headers = List[String]
  type Data    = List[List[Object]]

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
    kvLess: Seq[(String, String)]
  ): IO[(Headers, Data)] = {
    val tableName = "test"
    val kvEqQ =
      s"${kvEq.map { case (k, v) => k + "=" + v }.mkString("\n AND ")}"
    val kvMoreQ =
      s"${kvMore.map { case (k, v) => k + ">" + v }.mkString("\n AND ")}"
    val kvLessQ =
      s"${kvLess.map { case (k, v) => k + "<" + v }.mkString("\n AND ")}"

    val whereFragment =
      if (kvEq.nonEmpty || kvLess.nonEmpty || kvMore.nonEmpty)
        s"where \n$kvEqQ \n AND $kvMoreQ \n AND $kvLessQ"
      else ""

    Fragment
      .const(s"select ${fields.mkString(",")} from $tableName $whereFragment")
      .execWith(exec)
      .transact(xa)
  }

  // Exec our PreparedStatement, examining metadata to figure out column count.
  private def exec: PreparedStatementIO[(Headers, Data)] =
    for {
      md <- HPS.getMetaData // lots of useful info here
      cols = (1 to md.getColumnCount).toList
      data <- HPS.executeQuery(readAll(cols))
    } yield (cols.map(md.getColumnName), data)

  // Read the specified columns from the resultset.
  private def readAll(cols: List[Int]): ResultSetIO[Data] =
    readOne(cols).whileM[List](HRS.next)

  // Take a list of column offsets and read a parallel list of values.
  private def readOne(cols: List[Int]): ResultSetIO[List[Object]] =
    cols.traverse(FRS.getObject) // always works
}
