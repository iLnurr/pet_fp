package com.iserba.fp

import com.iserba.fp.utils.{Monad, StreamProcess}
import com.iserba.fp.utils.StreamProcess.Emit
import com.iserba.fp.utils.StreamProcessHelper._

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