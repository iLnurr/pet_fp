import cats.effect.IO
import doobie._
import doobie.implicits._
import conf._
import com.typesafe.scalalogging.Logger
import cats.implicits._
import doobie.util.transactor.Transactor.Aux

object db {
  // https://github.com/tpolecat/doobie/blob/master/modules/example/src/main/scala/example/Dynamic.scala
  type Headers = List[String]
  type Data    = List[List[Object]]

  val dbLogger         = Logger("db")
  implicit lazy val cs = IO.contextShift(ExecutionContexts.synchronous)

  def startTransactor(
    driver: String = dbDriver,
    url:    String = dbUrl,
    user:   String = dbUser,
    pass:   String = dbPass
  ): Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      driver = driver,
      url    = url,
      user   = user,
      pass   = pass
    )

  def getRecords(
    query:       String
  )(implicit xa: Transactor[IO]): IO[(Headers, Data)] =
    Fragment
      .const(query)
      .execWith(exec)
      .transact(xa)
      .handleErrorWith { ex =>
        dbLogger.error(s"Can't process query $query", ex)
        IO.raiseError(ex)
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
