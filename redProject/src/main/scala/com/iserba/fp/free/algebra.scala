package com.iserba.fp.free

import com.iserba.fp.ResponseStream
import com.iserba.fp.free.algebra._
import com.iserba.fp.model._
import com.iserba.fp.utils.Free
import com.iserba.fp.utils.Free.~>
import scala.language.{higherKinds, implicitConversions}

import scala.concurrent.Future

object algebra {

  sealed trait ConnectionAlg[T]

  case class GetRequest(timeout: Long) extends ConnectionAlg[Option[Request]]

  case class AddResponse(resp: Response, req: Request) extends ConnectionAlg[Response]

  case class MakeRequestAsync(r: Request) extends ConnectionAlg[Unit]

  case object CloseConnection extends ConnectionAlg[Unit]

  type Connection[T] = Free[ConnectionAlg, T]

  def getRequest(timeout: Long): Connection[Option[Request]] =
    Free.liftF(GetRequest(timeout))

  def addResponse(resp: Response, req: Request): Connection[Response] =
    Free.liftF(AddResponse(resp, req))

  def makeRequestAsync(r: Request): Connection[Unit] =
    Free.liftF(MakeRequestAsync(r))

  def close(): Connection[Unit] =
    Free.liftF(CloseConnection)

  def checkRequest(r: Request): Connection[Request] = makeRequestAsync(r).flatMap(_ =>
    getRequest(10000).map {
      case Some(value) =>
        require(value == r)
        value
      case None =>
        sys.error(s"$r not fired")
    }
  )
  lazy val checkResult = checkRequest(Request(Some(Event(23,Model(Some(0)))))).foldMap(interpreters.futureInterpreter)

  sealed trait ServerAlg[T]

  case class Convert(r: Request) extends ServerAlg[Response]

  case class RunProcess[S]() extends ServerAlg[S]

  type Server[T] = Free[ServerAlg, T]

  def convert(r: Request): Server[Response] =
    Free.liftF(Convert(r))

  def runProcess[F[_]]()(implicit conn: Connection[Response]): Server[ResponseStream[F]] =
    Free.liftF(RunProcess[ResponseStream[F]]())

  sealed trait ClientAlg[T]

  case class Call[F[_]](r: Request) extends ClientAlg[ResponseStream[F]]

  type Client[T] = Free[ClientAlg, T]

  def call[F[_]](r: Request)(implicit conn: Connection[Response]): Client[ResponseStream[F]] =
    Free.liftF(Call[F](r))
}

object interpreters {

  import scala.concurrent.ExecutionContext.Implicits.global

  case class RequestState(occupied: Boolean, resp: Option[Response] = None)

  private var requestCache = Map[Request, RequestState]()
  val futureInterpreter = new (ConnectionAlg ~> Future) {
    override def apply[A](f: ConnectionAlg[A]): Future[A] = f match {
      case GetRequest(timeout@_) =>
        val maybeRequest: Option[Request] = requestCache
          .find {
            case (request@_, requestState) => !requestState.occupied
          }
          .map {
            case (request, tuple@_) =>
              requestCache = requestCache.updated(request, RequestState(occupied = true))
              request
          }
        Future.successful(maybeRequest)
      case AddResponse(resp, req) =>
        requestCache = requestCache.updated(req, RequestState(occupied = true, Some(resp)))
        Future(resp)
      case MakeRequestAsync(r) =>
        requestCache = requestCache.updated(r, RequestState(occupied = false))
        Future.unit
      case CloseConnection =>
        requestCache = Map.empty[Request, RequestState]
        Future.unit
    }
  }
}

