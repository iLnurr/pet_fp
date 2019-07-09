package com.iserba.fp

import com.iserba.fp.algebra._

import scala.concurrent.{ExecutionContext, Future, Promise}

object impl {

  case class ParFuture(implicit E: ExecutionContext) extends Par[Future] {
    def unit[A](a: => A): Future[A] =
      Future.successful(a)
    def delay[A](a: => A): Future[A] =
      Future.successful(a)
    def fork[A](a: => Future[A]): Future[A] =
      a.flatMap(eval(_))
    def lazyUnit[A](a: => A): Future[A] =
      fork(unit(a))
    def flatMap[A, B](a: Future[A])(f: A => Future[B]): Future[B] =
      a.flatMap(f)
    def async[A](f: (A => Unit) => Unit): Future[A] = {
      val p = Promise[A]()
      f.apply(a => p.success(a))
      p.future
    }
    def asyncF[A, B](f: A => B): A => Future[B] = a =>
      lazyUnit(f(a))
    def async[A](f: (Either[Throwable,A] => Unit) => Unit): Future[A] = {
      val p = Promise[A]()
      f.apply{
        case Right(a) => p.success(a)
        case Left(e) => p.failure(e)
      }
      p.future
    }
    def attempt[A](a: Future[A]): Future[Either[Throwable, A]] =
      a.map(Right(_)).recover{case ex => Left(ex)}
    def fail[A](t: Throwable): Future[A] =
      Future.failed(t)
    def eval[A](r: => A): Future[A] =
      Future.apply(r)

    import scala.concurrent.duration._
    def parRun[A](p: Future[A]): A =
      scala.concurrent.Await.result(p, Duration.Inf)
  }

  implicit def scalaFuture(implicit ec: ExecutionContext): MonadCatch[Future] = new MonadCatch[Future] {
    def attempt[A](a: Future[A]): Future[Either[Throwable, A]] =
      a.map(Right(_)).recoverWith {case ex => Future.successful(Left(ex))}
    def fail[A](t: Throwable): Future[A] =
      Future.failed[A](t)
    def unit[A](a: => A): Future[A] =
      Future.successful(a)
    def flatMap[A, B](a: Future[A])(f: A => Future[B]): Future[B] =
      a.flatMap(f)
  }

}
