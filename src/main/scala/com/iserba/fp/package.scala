package com.iserba

import com.iserba.fp.utils.Free.Suspend
import com.iserba.fp.utils.Monad.MonadCatch
import com.iserba.fp.utils.{Free, Monad, Par}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.higherKinds

package object fp {
  type ParF[+A] = Future[A]
  type IO[A] = Free[ParF,A]

  implicit val ioMonad: Monad[({type f[a] = Free[ParF, a]})#f] = Free.freeMonad[ParF]
  implicit val ioMC: MonadCatch[({type f[a] = Free[ParF, a]})#f] = new MonadCatch[({type f[a] = Free[ParF, a]})#f] {
    def attempt[A](a: Free[ParF, A]): Free[ParF, Either[Throwable, A]] = {
      val r = Free.run(a) map {a =>
        Right(a)
      } recover {case thr => Left(thr)}
      par(r)
    }
    def fail[A](t: Throwable): Free[ParF, A] =
      par(Future.failed[A](t))
    override def unit[A](a: => A): Free[ParF, A] =
      ioMonad.unit(a)
    override def flatMap[A, B](a: Free[ParF, A])(f: A => Free[ParF, B]): Free[ParF, B] =
      ioMonad.flatMap(a)(f)
  }
  implicit val parFuture: Par[ParF] = impl.parFuture

  def IO[A](a: => A): IO[A] =
    Suspend { parFuture.delay(a) }
  def now[A](a: A): IO[A] =
    Return(a)
  def fork[A](a: => IO[A]): IO[A] =
    par(parFuture.lazyUnit(())) flatMap (_ => a)
  def forkUnit[A](a: => A): IO[A] =
    fork(now(a))
  def delay[A](a: => A): IO[A] =
    now(()) flatMap (_ => now(a))
  def par[A](a: ParF[A]): IO[A] =
    Suspend(a)
  def async[A](cb: ((A => Unit) => Unit)): IO[A] =
    fork(par(parFuture.async(cb)))
  def Return[A](a: A): IO[A] =
    utils.Free.Return[ParF,A](a)

  // To run an `IO`, we need an executor service.
  // The name we have chosen for this method, `unsafePerformIO`,
  // reflects that is is unsafe, i.e. that it has side effects,
  // and that it _performs_ the actual I/O.
  import java.util.concurrent.ExecutorService
  def unsafePerformIO[A](io: IO[A])(implicit E: ExecutorService): A =
    parFuture.parRun { Free.run(io)(parFuture) }
}
