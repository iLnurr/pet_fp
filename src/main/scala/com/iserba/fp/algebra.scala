package com.iserba.fp

import scala.language.{higherKinds, implicitConversions}
import java.lang.{Process => _}
import utils.StreamProcess

object algebra {
  trait Tpe
  trait Metadata
  trait Model
  case class Event(tpe: Tpe, ts: Long, metadata: Metadata, json: Model)

  trait Request[A]
  trait Response[A]

  type Req = Request[Event]
  type Resp = Response[Event]

  trait Server[F[_]] {
    def convert: Req => Resp
    def run: StreamProcess[F, Req => Resp]
  }

  trait Client[F[_]] {
    def request(create: => Req): StreamProcess[F,Req]
    def call(request: => StreamProcess[F,Req]): StreamProcess[F,Resp]
  }
}