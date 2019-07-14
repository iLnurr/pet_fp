package com.iserba.fp

import com.iserba.fp.utils.StreamProcess
import com.iserba.fp.utils.StreamProcess.Emit
import com.iserba.fp.utils.StreamProcessHelper._

import scala.language.{higherKinds, implicitConversions}

object algebra {
  trait Tpe
  trait Metadata {
    def status: Int
  }
  sealed trait Model {
    def id: Option[Long]
  }
  case class Event(tpe: Tpe, ts: Long, metadata: Metadata, model: String)

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
    def sendRequest(r: Req): Resp
    def close: Unit = {
      println(s"Close connection")
    }
  }

  trait Server[F[_]] {
    def conn: IO[Connection]
    def convert: Req => Resp
    def run(): StreamProcess[IO,Resp] =
      resource_(conn){c =>
        def step = c.getRequest
        def requests: StreamProcess[IO, Resp] = eval(IO(step)).flatMap {
          case None =>
            println(s"Server wait for requests")
            run()
          case Some(req) =>
            println(s"Server got request ${req}")
            val resp = convert(req)
            println(s"Server response ${req}")
            c.addResponse(resp,req)
            Emit(resp, requests)
        }
        requests
      }{
        c => IO(())
      }

  }

  trait Client[F[_]] {
    def conn: IO[Connection]
    def call(request: Req): StreamProcess[IO,Resp] =
      resource(conn){c =>
        eval(IO{
          println(s"Client send request $request")
          val resp = c.sendRequest(request)
          println(s"Client got response $resp")
          resp
        })
      }{
        c => eval_(IO(c.close))
      }
  }
}