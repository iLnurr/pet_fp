package test

import java.util.concurrent.atomic.AtomicLong

import com.iserba.fp._
import algebra._
import com.iserba.fp.utils.{Monad, StreamProcess}
import com.iserba.fp.utils.StreamProcess.Emit
import com.iserba.fp.utils.StreamProcessHelper.{eval, eval_, resource}
import test.TestImpl._

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object algebra {
  trait Model {
    def id: Option[Long]
  }
  case class Event(ts: Long, model: Model)

  trait Request[F[_],A] {
    def entity: F[A]
  }
  trait Response[F[_],A] {
    def body: F[A]
  }

  type Req = Request[Option, Event]
  type Resp = Response[Option, Event]
  trait Connection {
    def getRequest: Option[Req]
    def addResponse(resp: Resp, req: Req): Resp
    def makeRequest(r: Req): Resp
    def close(): Unit = {
      println(s"Close connection")
    }
  }

  trait Server[F[_]] {
    def convert: Req => Resp
    def run(conn: F[Connection])(implicit F: Monad[F]): StreamProcess[F,Resp] =
      resource(conn){c =>
        def step = c.getRequest
        def requests: StreamProcess[F, Resp] = eval(F.unit(step)).flatMap {
          case None =>
            println(s"Server wait for requests")
            run(conn)
          case Some(req) =>
            println(s"Server got request $req")
            val resp = convert(req)
            println(s"Server response $resp")
            c.addResponse(resp,req)
            Emit(resp, requests)
        }
        requests
      }{
        c => eval_(F.unit(c.close()))
      }

  }

  trait Client[F[_]] {
    def call(request: Req, conn: F[Connection])(implicit F: Monad[F]): StreamProcess[F,Resp] =
      resource(conn){c =>
        eval(F.unit{
          println(s"Client send request $request")
          val resp = c.makeRequest(request)
          println(s"Client got response $resp")
          resp
        })
      }{
        _ => eval_(F.unit(println(s"Client call ended")))
      }
  }
}
object Test extends App {
  import parFIO._
  val testConnection: IO[Connection] = IO(new TestConnection)
  val server = new Server[IO] {
    override def convert: Req => Resp = TestImpl.convert
  }
  val client1 = new Client[IO]{}
  val client2 = new Client[IO]{}

  val serverProgram =
    server
      .run(testConnection)
      .runStreamProcess
      .runFree

  val client1Program =
    client1.call(testRequest, testConnection)
      .runStreamProcess
      .runFree

  val client2Program =
    client2.call(testRequest, testConnection)
      .runStreamProcess
      .runFree

  val main = for {
    _ <- serverProgram
    _ <- client1Program
    _ <- client2Program
  } yield ()

  Await.result(main, Duration.Inf)

}

object TestImpl {
  case class RequestImpl[A](entity: Option[A]) extends Request[Option, A]
  case class ResponseImpl[A](body: Option[A]) extends Response[Option, A]
  case class TestModel(id: Option[Long], info: String) extends Model

  def ts = System.currentTimeMillis()
  private val idAccumulator = new AtomicLong(0L)
  def testModel(id: Long = idAccumulator.getAndIncrement()): Model = TestModel(Some(id), "")
  def eventGen: Event = Event(ts = ts, model = testModel())
  def eventsF = List(eventGen)
  def testRequest: Req = RequestImpl(Some(eventGen))

  class TestConnection extends Connection {
    private var requests = List[Req]()
    private val responses = mutable.Map[Req,Resp]()

    def getRequest = {
      Thread.sleep(1000)
      requests.headOption.map { r =>
        requests = if (requests.isEmpty) Nil else requests.tail
        r
      }
    }
    def makeRequest(req: Req): Resp = {
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
      responses.update(req,resp)
      resp
    }
  }

  def convert: Req => Resp = req =>
    ResponseImpl(req.entity.map{ event =>
      event.copy(model = TestModel(event.model.id, "updated model"))
    })
}