package com.iserba.fp.utils

import scala.language.{higherKinds, implicitConversions}

trait Functor[F[_]] {
  def map[A,B](a: F[A])(f: A => B): F[B]
}
