package com.iserba.fp.utils

import com.iserba.fp.utils
import com.iserba.fp.utils.Free.Suspend
import com.iserba.fp.utils.Monad.MonadCatch

import scala.util.{ Failure, Success, Try }

trait IOWrap[F[_]] {
  type IO[A] = Free[F, A]
  implicit def parF: Par[F]
  implicit val ioFMonad: Monad[Free[F, *]] =
    Free.freeMonad[F]
  implicit val ioMC: MonadCatch[Free[F, *]] =
    new MonadCatch[Free[F, *]] {
      def attempt[A](a: Free[F, A]): Free[F, Either[Throwable, A]] =
        Try(
          parF.parRun(
            Free.run(a)
          )
        ) match {
          case Success(aa) =>
            IO(Right[Throwable, A](aa))
          case Failure(exception) =>
            IO(Left[Throwable, A](exception))
        }

      def fail[A](t: Throwable): Free[F, A] =
        throw t
      override def unit[A](a: => A): Free[F, A] =
        Return(a)
      override def flatMap[A, B](
          a: Free[F, A]
      )(f: A => Free[F, B]): Free[F, B] =
        a.flatMap(f)
    }
  def IO[A](a: => A): IO[A] =
    Suspend(parF.delay(a))
  def now[A](a: A): IO[A] =
    Return(a)
  def fork[A](a: => IO[A]): IO[A] =
    fa(parF.lazyUnit(())).flatMap(_ => a)
  def forkUnit[A](a: => A): IO[A] =
    fork(now(a))
  def delay[A](a: => A): IO[A] =
    now(()).flatMap(_ => now(a))
  def fa[A](a: F[A]): IO[A] =
    Suspend(a)
  def async[A](cb: ((A => Unit) => Unit)): IO[A] =
    fork(fa(parF.async(cb)))
  def Return[A](a: A): IO[A] =
    utils.Free.Return(a)

  def unsafePerformIO[A](io: IO[A]): A =
    parF.parRun(
      Free.run(io)
    )

}
object IOWrap {
  def apply[F[_]](implicit par: Par[F]): IOWrap[F] = new IOWrap[F] {
    implicit val parF: Par[F] = par
  }
}
