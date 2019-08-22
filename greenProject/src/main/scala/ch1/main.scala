package ch1

import ch1.algebra.PrintableSyntax._
import ch1.domain._
import ch1.impl._

object main extends App {
  import ch1.algebra.Printable._
  val cat = Cat("c", 1, "red")

  print(cat)
  cat.print

  import cats.Show
  import cats.instances.int._
  import cats.instances.string._
  import cats.syntax.show._
  import impl.catShow

  val showInt: Show[Int] = Show.apply[Int]
  val showString: Show[String] = Show.apply[String]

  showInt.show(123)
  123.show

  println(cat.show)


  import cats.Eq
  import cats.instances.int._
  import cats.instances.option._
  import cats.syntax.eq._

  val eqInt = Eq[Int]
  eqInt.eqv(123,123)
  eqInt.eqv(123,124)

  123 === 123
  123 =!= 124

  Option(1) =!= Option.empty[Int]

  import impl.catEq
  val cat2 = Cat("c2", 2, "black")
  cat =!= cat2
  Option(cat) =!= Option.empty[Cat]
  Option(cat) =!= Option(cat2)

  import cats.syntax.functor._
//  Branch(Leaf(10), Leaf(20)).map(_ * 2)
  Tree.branch(Tree.leaf(10), Tree.leaf(20)).map(_ * 2)

  format(Box("hello world"))

  import ch1.algebra.Codec._
  import ch1.algebra.CodecSyntax._
  encode(123.4)
  123.4.encode
  // res0: String = 123.4
  decode[Double]("123.4")
  // res1: Double = 123.4
  encode(Box(123.4))
  // res2: String = 123.4
  decode[Box[Double]]("123.4")
  // res3: Box[Double] = Box(123.4)
}
