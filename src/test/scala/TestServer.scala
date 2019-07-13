package test

import java.util.concurrent.atomic.AtomicLong

import com.iserba.fp._
import com.iserba.fp.algebra._
import com.iserba.fp.utils.StreamProcess
import com.iserba.fp.utils.StreamProcess.{Emit, Halt}
import com.iserba.fp.utils.StreamProcessHelper._
import test.TestImpl._

import scala.language.{higherKinds, implicitConversions}

object Test extends App {
  val server = new ServerImpl
  server.run().map(r => println(s"Server response $r")).runLog
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

  def testStreamProcess: StreamProcess[IO, Event] =
    resource(eventsIO){ eventsStream =>
      lazy val iter = eventsStream.iterator
      def step = if (iter.hasNext) Some(iter.next) else None
      lazy val events: StreamProcess[IO, Event] = eval(IO(step)).flatMap {
        case None => Halt(End)
        case Some(evs) => Emit(evs, events)
      }
      events
    }{ _ =>
      eval_(IO(()))
    }

  class ConnectionImpl extends Connection {
    private var underlying = List[Req](testRequest)
    def requests: List[Req] =
      get
    def get: List[Req] =
      underlying
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
