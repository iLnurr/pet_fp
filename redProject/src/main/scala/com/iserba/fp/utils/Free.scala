package com.iserba.fp.utils

import Free._

sealed trait Free[F[_],A] { self =>
  def unit(a: => A): Free[F,A] =
    Return[F,A](a)
  def flatMap[B](f: A => Free[F,B]): Free[F,B] =
    FlatMap(this, f)
  def map[B](f: A => B): Free[F,B] =
    flatMap(f andThen (Return(_)))
  def runFree(implicit F: Monad[F]): F[A] =
    Free.run(self)
  def foldMap[M[_]](f: Translate[F, M])(implicit M: Monad[M]): M[A] =
    Free.runFree(this)(f)
}
object Free {
  case class Return[F[_],A](a: A) extends Free[F, A]
  case class Suspend[F[_],A](s: F[A]) extends Free[F, A]
  case class FlatMap[F[_],A,B](s: Free[F, A],
                               f: A => Free[F, B]) extends Free[F, B]

  def pure[F[_],A](a: A): Free[F,A] =
    Return[F,A](a)
  def liftF[F[_],A](f: F[A]): Free[F,A] =
    Suspend[F,A](f)

  def freeMonad[F[_]]: Monad[({type f[a] = Free[F,a]})#f] = new Monad[({type f[a] = Free[F, a]})#f] {
    override def unit[A](a: => A): Free[F, A] =
      Return[F,A](a)
    override def flatMap[A, B](fa: Free[F, A])(f: A => Free[F, B]): Free[F, B] =
      fa.flatMap(f)
  }

  import scala.reflect.{ClassTag, classTag}
  // @annotation.tailrec
  def runTrampoline[A: ClassTag](a: Free[Function0,A]): A = a match {
    case Return(value) => value
    case Suspend(s) => s()
    case FlatMap(x,f) => x match {
      case Return(aa:A) if classTag[A].runtimeClass.isInstance(aa) => runTrampoline(f(aa))
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


//  def translate[F[_],G[_],A](f: Free[F,A])(fg: F ~> G): Free[G,A] = {
//    type FreeG[A] = Free[G,A]
//    val tr = new Translate[F,FreeG] {
//      override def apply[A](f: F[A]): Free[G,A] = Suspend(fg(f))
//    }
//
//    runFree(f)(tr)(freeMonad[G])
//  }
}
