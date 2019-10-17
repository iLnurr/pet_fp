package com.iserba

import com.iserba.fp.model.Response
import com.iserba.fp.utils.{IOWrap, Monad, StreamProcess}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

package object fp {
  type ResponseStream[F[_]] = StreamProcess[F,Response]

  type ParF[+A] = Future[A]
  implicit val monadImpl: Monad[ParF] = instances.FutureM.monadFuture
  implicit val monadCatchImpl: Monad.MonadCatch[ParF] = instances.FutureM.monadCatchFuture
  val parFIO: IOWrap[ParF] = IOWrap[ParF](instances.FutureM.parFuture)

  type IO[A] = parFIO.IO[A]// Free[ParF,A]
  def IO[A](a: => A): IO[A] =
    parFIO.IO(a)
}
