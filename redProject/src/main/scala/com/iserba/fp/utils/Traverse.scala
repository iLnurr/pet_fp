package com.iserba.fp.utils

import scala.language.{higherKinds, implicitConversions}

trait Traverse[F[_]] extends Functor[F] { self =>
  type Id[A] = A
  val idMonad: Monad[Id] = new Monad[Id] {
    def unit[A](a: => A) = a
    def flatMap[A,B](a: A)(f: A => B): B = f(a)
  }
  def traverse[M[_]:Applicative,A,B](fa: F[A])(f: A => M[B]): M[F[B]]
  def sequence[M[_]:Applicative,A](fma: F[M[A]]): M[F[A]] =
    traverse(fma)(ma => ma)
  def map[A,B](fa: F[A])(f: A => B): F[B] =
    traverse[Id, A, B](fa)(f)(idMonad)
  def compose[G[_]](implicit G: Traverse[G]): Traverse[({type f[x] = F[G[x]]})#f] =
    new Traverse[({type f[x] = F[G[x]]})#f] {
      def traverse[M[_]:Applicative,A,B](fa: F[G[A]])(f: A => M[B]): M[F[G[B]]] =
        self.traverse(fa)((ga: G[A]) => G.traverse(ga)(f))
    }
}
