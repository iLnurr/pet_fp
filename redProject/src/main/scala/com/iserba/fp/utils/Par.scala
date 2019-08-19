package com.iserba.fp.utils

import Monad._

trait Par[F[_]] extends MonadCatch[F] {
  def lazyUnit[A](a: => A): F[A]
  def delay[A](a: => A): F[A]
  def fork[A](a: => F[A]): F[A]
  def async[A](f: (A => Unit) => Unit): F[A]
  def asyncF[A,B](f: A => B): A => F[B]
  def asyncTask[A](f: (Either[Throwable,A] => Unit) => Unit): F[A]
  /**
    * Helper function, for evaluating an action
    * asynchronously.
    */
  def eval[A](r: => A): F[A]
  def parRun[A](p: F[A]): A
}
