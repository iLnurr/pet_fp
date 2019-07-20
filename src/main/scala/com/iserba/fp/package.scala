package com.iserba

import com.iserba.fp.utils.{IOWrap, Monad, Par}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.higherKinds

package object fp {
  type ParF[+A] = Future[A]
  implicit val monadImpl: Monad[ParF] = impl.FutureM.monadFuture
  implicit val monadCatchImpl: Monad.MonadCatch[ParF] = impl.FutureM.monadCatchFuture
  val parFIO: IOWrap[ParF] = new IOWrap[ParF] {
    implicit val parF: Par[ParF] = impl.FutureM.parFuture
  }
  import parFIO._

  type IO[A] = parFIO.IO[A]// Free[ParF,A]
  def IO[A](a: => A): IO[A] =
    parFIO.IO(a)
}
