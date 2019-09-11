package catsex.ch11

import cats.kernel.CommutativeMonoid

object algebra {
  trait BoundedSemiLattice[A] extends CommutativeMonoid[A] {
    def combine(a1: A, a2: A): A
    def empty: A
  }
}
