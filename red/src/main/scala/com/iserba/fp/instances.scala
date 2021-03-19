package com.iserba.fp

import com.iserba.fp.utils.Monad.MonadCatch
import com.iserba.fp.utils.{ Monad, Par }

import scala.concurrent.{ ExecutionContext, Future, Promise }

object instances {
  object FutureM {
    implicit def parFuture(implicit ec: ExecutionContext): Par[Future] =
      new Par[Future] {
        def unit[A](a: => A): Future[A] =
          Future.successful(a)
        def delay[A](a: => A): Future[A] =
          lazyUnit(a)
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
        def asyncF[A, B](f: A => B): A => Future[B] = a => lazyUnit(f(a))
        def asyncTask[A](
            f: (Either[Throwable, A] => Unit) => Unit
        ): Future[A] = {
          val p = Promise[A]()
          f.apply {
            case Right(a) => p.success(a)
            case Left(e)  => p.failure(e)
          }
          p.future
        }
        def attempt[A](a: Future[A]): Future[Either[Throwable, A]] =
          monadCatchFuture.attempt(a)
        def fail[A](t: Throwable): Future[A] =
          monadCatchFuture.fail(t)
        def eval[A](r: => A): Future[A] =
          Future.apply(r)

        import scala.concurrent.duration._
        def parRun[A](p: Future[A]): A =
          scala.concurrent.Await.result(p, Duration.Inf)
      }

    implicit def monadCatchFuture(
        implicit ec: ExecutionContext
    ): MonadCatch[Future] =
      new MonadCatch[Future] {
        def attempt[A](a: Future[A]): Future[Either[Throwable, A]] =
          a.map(Right(_)).recoverWith { case ex => Future.successful(Left(ex)) }
        def fail[A](t: Throwable): Future[A] =
          Future.failed[A](t)
        def unit[A](a: => A): Future[A] =
          Future.successful(a)
        def flatMap[A, B](a: Future[A])(f: A => Future[B]): Future[B] =
          a.flatMap(f)
      }

    implicit def monadFuture(implicit ec: ExecutionContext): Monad[Future] =
      new Monad[Future] {
        override def unit[A](a: => A): Future[A] =
          Future.successful(a)
        override def flatMap[A, B](a: Future[A])(f: A => Future[B]): Future[B] =
          a.flatMap(f)
      }
  }

}
