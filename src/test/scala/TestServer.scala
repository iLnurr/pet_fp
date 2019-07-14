package test

import java.util.concurrent.atomic.AtomicLong

import com.iserba.fp._
import com.iserba.fp.algebra._
import test.TestImpl._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.{higherKinds, implicitConversions}

object Test extends App {
  def runServer = Future {
    val server = new ServerImpl
    server.run().runLog
  }
  val client = Future(new ClientImpl)
  def makeReq = {
    client.map(_.call(testRequest).runLog)
  }
  runServer
  makeReq

  Thread.sleep(10000)
  makeReq
  makeReq
  makeReq
  Thread.sleep(10000)

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
    private var requests = List[Req]()
    private val responses = mutable.Map[Req,Resp]()

    def getRequest = {
      Thread.sleep(1000)
      requests.headOption.map { r =>
        requests = if (requests.isEmpty) Nil else requests.tail
        r
      }
    }
    def sendRequest(req: Req): Resp = {
      def tryToGet(req: Req): Resp = {
        responses.getOrElse(req, {
          Thread.sleep(1000)
          tryToGet(req)
        })
      }
      val nl = req :: requests
      requests = nl
      tryToGet(req)
    }

    def addResponse(resp: Resp, req: Req): Resp = {
      responses.addOne(req,resp)
      resp
    }
  }
  val connection = new ConnectionImpl

  class ServerImpl extends Server[IO] {
    def conn: IO[Connection] =
      IO(connection)
    def convert: Req => Resp = req =>
      ResponseImpl(req.entity.map{ event =>
        val old = event.model
        event.copy(model = old + " updated model")
      })
  }
  class ClientImpl extends Client[IO] {
    def conn: IO[Connection] =
      IO(connection)
  }
}
