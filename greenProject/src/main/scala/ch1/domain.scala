package ch1

object domain {
  final case class Cat(name: String, age: Int, color: String)

  sealed trait Tree[+A]
  object Tree {
    def branch[A](left: Tree[A], right: Tree[A]): Tree[A] =
      Branch(left,right)
    def leaf[A](a: A): Tree[A] =
      Leaf(a)
  }
  final case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
  final case class Leaf[A](value: A) extends Tree[A]

  final case class Box[A](value: A)
}
