package com.iserba.fp.utils

trait Applicative[F[_]] extends Functor[F] {
  def unit[A](a: => A): F[A]
  def apply[A,B](fab: F[A => B])(fa: F[A]): F[B]
  def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]
}
