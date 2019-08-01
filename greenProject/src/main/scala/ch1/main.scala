package ch1

import ch1.domain.Cat
import impl.catPrintable
import PrintableSyntax._

object main extends App {
  import Printable._
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
}
