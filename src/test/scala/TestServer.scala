package test

import java.util.concurrent.atomic.AtomicLong

import com.iserba.fp._
import com.iserba.fp.algebra._
import test.TestImpl._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{higherKinds, implicitConversions}

object Test extends App {
  val server = new ServerImpl
  def runServer = server.run().map(r => println(s"Server response $r")).runLog
  connection.add(testRequest)
  runServer
  connection.add(testRequest)


}
object TestImpl {
  case class RequestImpl[A](entity: Option[A]) extends Request[Option, A]
  case class ResponseImpl[A](body: Option[A]) extends Response[Option, A]

  case object StringTpe extends Tpe
  case class TestMeta(status: Int) extends Metadata
  def ts = System.currentTimeMillis()
  val idAccumulator = new AtomicLong(0L)
  def testModel(id: Long = idAccumulator.getAndIncrement()) = s"{id = $id}"
  def eventGen: Event = new Event(tpe = StringTpe, metadata = TestMeta(200), ts = ts, model = testModel())

  def eventsF = List(eventGen)
  def eventsIO = IO(eventsF)

  def testRequest: Req =
    RequestImpl(Some(eventGen))

  class ConnectionImpl extends Connection {
    private var underlying = List[Req]()
    def request = {
      get.map { r =>
        underlying = if (underlying.isEmpty) Nil else underlying.tail
        r
      }
    }
    def get: Option[Req] =
      underlying.headOption
    def add(req: Req): Unit = {
      val nl = req :: underlying
      underlying = nl
    }
  }
  val connection = new ConnectionImpl

  class ServerImpl extends Server[IO] {
    def conn: IO[Connection] =
      IO(connection)
    def convert: Req => Resp = req =>
      ResponseImpl(req.entity.map(_.copy(model = "updated model")))
  }
}
