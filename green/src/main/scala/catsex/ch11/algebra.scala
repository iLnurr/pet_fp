package catsex.ch11

import cats.kernel.CommutativeMonoid

object algebra {
  trait BoundedSemiLattice[A] extends CommutativeMonoid[A] {
    def combine(a1: A, a2: A): A
    def empty: A
  }

  trait GCounter[F[_, _], K, V] {
    def increment(f: F[K, V])(k: K, v: V)(
      implicit m:    CommutativeMonoid[V]
    ): F[K, V]
    def merge(f1: F[K, V], f2: F[K, V])(
      implicit b: BoundedSemiLattice[V]
    ): F[K, V]
    def total(f: F[K, V])(implicit m: CommutativeMonoid[V]): V
  }
  object GCounter {
    def apply[F[_, _], K, V](
      implicit counter: GCounter[F, K, V]
    ): GCounter[F, K, V] =
      counter
  }

  trait KeyValueStore[F[_, _]] {
    def put[K, V](f:       F[K, V])(k: K, v: V): F[K, V]
    def get[K, V](f:       F[K, V])(k: K): Option[V]
    def getOrElse[K, V](f: F[K, V])(k: K, default: V): V =
      get(f)(k).getOrElse(default)
    def values[K, V](f: F[K, V]): List[V]
  }
  object KeyValueStore {}
}
