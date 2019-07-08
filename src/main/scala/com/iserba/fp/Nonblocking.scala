package com.iserba.fp

import scala.language.implicitConversions

object Nonblocking {
  def unit[A](a: A): Par[A] =
    es => new Future[A] {
      def apply(cb: A => Unit): Unit =
        cb(a)
    }

  /** A non-strict version of `unit` */
  def delay[A](a: => A): Par[A] =
    es => new Future[A] {
      def apply(cb: A => Unit): Unit =
        cb(a)
    }

  def fork[A](a: => Par[A]): Par[A] =
    es => new Future[A] {
      def apply(cb: A => Unit): Unit =
        eval(es)(a(es)(cb))
    }


  // specialized version of `map`
  def map[A,B](p: Par[A])(f: A => B): Par[B] =
    es => new Future[B] {
      def apply(cb: B => Unit): Unit =
        p(es)(a => eval(es) { cb(f(a)) })
    }

  def lazyUnit[A](a: => A): Par[A] =
    fork(unit(a))

  def async[A](f: (A => Unit) => Unit): Par[A] = es => new Future[A] {
    def apply(k: A => Unit) = f(k)
  }
  def asyncF[A,B](f: A => B): A => Par[B] =
    a => lazyUnit(f(a))

  def flatMap[A,B](p: Par[A])(f: A => Par[B]): Par[B] = es => new Future[B] {
    def apply(cb: B => Unit): Unit = {
      p(es) { a =>
        f(a)(es).apply(cb)
      }
    }
  }

  def join[A](p: Par[Par[A]]): Par[A] = es => new Future[A] {
    def apply(cb: A => Unit): Unit = {
      p(es){ inner =>
        eval(es){
          inner(es)(cb)
        }
      }
    }
  }

  /* Gives us infix syntax for `Par`. */
  implicit def toParOps[A](p: Par[A]): ParOps[A] = new ParOps(p)

  // infix versions of `map`, `map2`
  class ParOps[A](p: Par[A]) {
    def map[B](f: A => B): Par[B] = Nonblocking.map(p)(f)
    def flatMap[B](f: A => Par[B]): Par[B] = Nonblocking.flatMap(p)(f)
  }
}

