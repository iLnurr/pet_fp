//package com.iserba
//
//import java.util
//import java.util.concurrent.{Callable, CountDownLatch, ExecutorService}
//
//import scala.language.higherKinds
//
//package object fp {
//  type Free[F[_], A] = IO3.Free[F, A]
//  type IO[A] = Free[Par, A]
//  def IO[A](a: => A): IO[A] = IO3.Suspend { Par(a) }
//
//  trait MFuture[A] {
//    def apply(cb: A => Unit): Unit
//  }
//  type Future[+A] = MFuture[A]
//  type Par[+A] = ExecutorService => Future[A]
//  def Par[A](a: => A): Par[A] =
//    Nonblocking.delay(a)
//  def parUnit[A](a: A): Par[A] =
//    Nonblocking.unit(a)
//  def parFork[A](a: => Par[A]): Par[A] =
//    Nonblocking.fork(a)
//  def parFlatMap[A,B](p: Par[A])(f: A => Par[B]): Par[B] =
//    Nonblocking.flatMap(p)(f)
//  def parLazyUnit(): Par[Unit] =
//    Nonblocking.lazyUnit(())
//  def parAsync[A](a: (A => Unit) => Unit) =
//    Nonblocking.async(a)
//  def parIO[A](a: => Par[A]): IO[A] =
//    IO3.Suspend(a)
//
//  /**
//    * Helper function, for evaluating an action
//    * asynchronously, using the given `ExecutorService`.
//    */
//  def eval[A](es: ExecutorService)(r: => A) =
//    es.submit(new Callable[A] { def call: A = r })
//  def parRun[A](es: ExecutorService)(p: Par[A]): A = {
//    val ref = new java.util.concurrent.atomic.AtomicReference[A]
//    val latch = new CountDownLatch(1)
//    p(es) { a => ref.set(a); latch.countDown }
//    latch.await
//    ref.get
//  }
//
//  implicit val parMonad: Monad[Par] = new Monad[Par] {
//    def unit[A](a: => A): Par[A] =
//      parUnit(a)
//    def flatMap[A,B](a: Par[A])(f: A => Par[B]): Par[B] =
//      parFork { parFlatMap(a)(f) }
//  }
//
//  def now[A](a: A): IO[A] =
//    IO3.Return(a)
//  def fork[A](a: => IO[A]): IO[A] =
//    parIO(parLazyUnit()) flatMap (_ => a)
//  def forkUnit[A](a: => A): IO[A] =
//    fork(now(a))
//  def delay[A](a: => A): IO[A] =
//    now(()) flatMap (_ => now(a))
//  def async[A](cb: (A => Unit) => Unit): IO[A] =
//    parIO(parAsync(cb))
//  def Suspend[A](a: A): IO[A] =
//    IO3.Suspend[Par,A](Par(a))
//  def Return[A](a: A): IO[A] =
//    IO3.Return[Par,A](a)
//
//  implicit val ioMonad = IO3.freeMonad[Par]
//
//  // To run an `IO`, we need an executor service.
//  // The name we have chosen for this method, `unsafePerformIO`,
//  // reflects that is is unsafe, i.e. that it has side effects,
//  // and that it _performs_ the actual I/O.
//  def unsafePerformIO[A](io: IO[A])(implicit E: ExecutorService): A =
//    parRun(E)(IO3.run(io)(parMonad))
//}
