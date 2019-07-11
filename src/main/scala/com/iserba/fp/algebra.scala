package com.iserba.fp

import scala.language.{higherKinds, implicitConversions}
import java.lang.{Process => _}
import utils.Process

object algebra {
  trait Server[F[_], Req, Resp] {
    def convert: Req => Resp
    def run: Process[F, Req => Resp]
  }
  trait Client[F[_], Req, Resp] {
    def request(create: => Req): Process[F,Req]
    def call(request: => Process[F,Req]): Process[F,Resp]
  }
}