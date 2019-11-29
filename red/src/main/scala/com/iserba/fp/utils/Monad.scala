package com.iserba.fp.utils

import scala.language.{higherKinds, implicitConversions}
import Monad._

trait Monad[F[_]] extends Applicative[F] {
  def unit[A](a:       => A): F[A]
  def flatMap[A, B](a: F[A])(f: A => F[B]): F[B]
  def join[A](mma:     F[F[A]]): F[A] =
    flatMap(mma)(ma => ma)
  def map[A, B](a: F[A])(f: A => B): F[B] =
    flatMap(a)(a => unit(f(a)))
  def map2[A, B, C](a: F[A], b: F[B])(f: (A, B) => C): F[C] =
    flatMap(a)(a => map(b)(b => f(a, b)))
  def apply[A, B](mf: F[A => B])(ma: F[A]): F[B] =
    flatMap(mf)(f => map(ma)(f))

  def as[A, B](a: F[A])(b: B): F[B] =
    map(a)(_ => b)
  def skip[A](a: F[A]): F[Unit] =
    as(a)(())
  def when[A](b: Boolean)(fa: => F[A]): F[Boolean] =
    if (b) as(fa)(true) else unit(false)
  def forever[A, B](a: F[A]): F[B] = {
    lazy val t: F[B] = a.flatMap(_ => t)
    t
  }
  def while_(a: F[Boolean])(b: F[Unit]): F[Unit] = {
    lazy val t: F[Unit] = while_(a)(b)
    a.flatMap(c => skip(when(c)(t)))
  }
  def doWhile[A](a: F[A])(cond: A => F[Boolean]): F[Unit] =
    for {
      a1 <- a
      ok <- cond(a1)
      _  <- if (ok) doWhile(a)(cond) else unit(())
    } yield ()
  def seq[A, B, C](f: A => F[B])(g: B => F[C]): A => F[C] =
    f.andThen(fb => flatMap(fb)(g))

  // syntax
  implicit def toMonadic[A](a: F[A]): Monadic[F, A] =
    new Monadic[F, A] { val F = Monad.this; def get = a }
}

object Monad {
  def apply[F[_]](implicit inst: Monad[F]): Monad[F] = inst
  trait Monadic[F[_], A] {
    val F: Monad[F]
    def get: F[A]
    private val a = get
    def map[B](f:     A => B): F[B] = F.map(a)(f)
    def flatMap[B](f: A => F[B]): F[B] = F.flatMap(a)(f)
    def **[B](b:      F[B]) = F.map2(a, b)((_, _))
    def *>[B](b:      F[B]) = F.map2(a, b)((_, b) => b)
    def map2[B, C](b: F[B])(f: (A, B) => C): F[C] = F.map2(a, b)(f)
    def as[B](b:      B): F[B] = F.as(a)(b)
    def skip: F[Unit] = F.skip(a)
  }

  /*
   * A context in which exceptions can be caught and
   * thrown.
   */
  trait MonadCatch[F[_]] extends Monad[F] {
    def attempt[A](a: F[A]):      F[Either[Throwable, A]]
    def fail[A](t:    Throwable): F[A]
  }
}
