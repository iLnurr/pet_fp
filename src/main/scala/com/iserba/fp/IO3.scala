package com.iserba.fp

import algebra._
import scala.language.{higherKinds, postfixOps}

object IO3 {
  def freeMonad[F[_]]: Monad[({type f[a] = Free[F,a]})#f] = new Monad[({type f[a] = Free[F, a]})#f] {
    override def unit[A](a: => A): Free[F, A] =
      Return[F,A](a)
    override def flatMap[A, B](fa: Free[F, A])(f: A => Free[F, B]): Free[F, B] =
      fa.flatMap(f)
  }

  // @annotation.tailrec
  def runTrampoline[A](a: Free[Function0,A]): A = a match {
    case Return(value) => value
    case Suspend(s) => s()
    case FlatMap(x,f) => x match {
      case Return(aa:A) => runTrampoline(f(aa))
      case Suspend(s) => runTrampoline(f(s()))
      case FlatMap(s, ff) => runTrampoline(s.flatMap(ff(_).flatMap(f)))
    }
  }

  def run[F[_],A](a: Free[F,A])(implicit F: Monad[F]): F[A] = step(a) match {
    case Return(aa) => F.unit(aa)
    case Suspend(s) => s
    case FlatMap(s, f) => s match {
      case Suspend(ss) => F.flatMap(ss)(aa => run(f(aa)))
      case _ => sys.error("Impossible, since `run` eliminates these cases")
    }
  }


  @annotation.tailrec
  def step[F[_],A](a: Free[F,A]): Free[F,A] = a match {
    case FlatMap(FlatMap(x, f), g) => step(x flatMap (a => f(a) flatMap g))
    case FlatMap(Return(x), f) => step(f(x))
    case _ => a
  }

  /* Translate between any `F[A]` to `G[A]`. */
  trait Translate[F[_], G[_]] { def apply[A](f: F[A]): G[A] }

  type ~>[F[_], G[_]] = Translate[F,G] // gives us infix syntax `F ~> G` for `Translate[F,G]`

  implicit val function0Monad = new Monad[Function0] {
    def unit[A](a: => A) = () => a
    def flatMap[A,B](a: Function0[A])(f: A => Function0[B]) =
      () => f(a())()
  }

  def runFree[F[_],G[_],A](free: Free[F,A])(t: F ~> G)(
                           implicit G: Monad[G]): G[A] =
    step(free) match {
      case Return(a) => G.unit(a)
      case Suspend(r) => t(r)
      case FlatMap(Suspend(r), f) => G.flatMap(t(r))(a => runFree(f(a))(t))
      case _ => sys.error("Impossible, since `step` eliminates these cases")
    }


  def translate[F[_],G[_],A](f: Free[F,A])(fg: F ~> G): Free[G,A] = {
    type FreeG[A] = Free[G,A]
    val tr = new Translate[F,FreeG] {
      override def apply[A](f: F[A]): Free[G,A] = Suspend(fg(f))
    }

    runFree(f)(tr)(freeMonad[G])
  }
}
