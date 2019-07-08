package com.iserba.fp

import language.higherKinds
import scala.concurrent.{ExecutionContext, Future => ScalaFuture}

/*
 * A context in which exceptions can be caught and
 * thrown.
 */
trait MonadCatch[F[_]] extends Monad[F] {
  def attempt[A](a: F[A]): F[Either[Throwable,A]]
  def fail[A](t: Throwable): F[A]
}

object MonadCatch {
  implicit def scalaFuture(implicit ec: ExecutionContext): MonadCatch[ScalaFuture] = new MonadCatch[ScalaFuture] {
    def attempt[A](a: ScalaFuture[A]): ScalaFuture[Either[Throwable, A]] =
      a.map(Right(_)).recoverWith {case ex => ScalaFuture.successful(Left(ex))}
    def fail[A](t: Throwable): ScalaFuture[A] =
      ScalaFuture.failed[A](t)
    def unit[A](a: => A): ScalaFuture[A] =
      ScalaFuture.successful(a)
    def flatMap[A, B](a: ScalaFuture[A])(f: A => ScalaFuture[B]): ScalaFuture[B] =
      a.flatMap(f)
  }
}
