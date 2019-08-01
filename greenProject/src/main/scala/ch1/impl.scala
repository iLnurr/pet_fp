package ch1

import ch1.domain.Cat

object impl {
  implicit val stringPrintable: Printable[String] = new Printable[String] {
    override def format(a: String): String =
      a
  }
  implicit val intPrintable: Printable[Int] = new Printable[Int] {
    override def format(a: Int): String =
      a.toString
  }
  implicit val catPrintable: Printable[Cat] = new Printable[Cat] {
    override def format(a: Cat): String =
      s"${Printable.format(a.name)} is a ${Printable.format(a.age)} year-old ${Printable.format(a.color)} cat."
  }

  import cats.Show
  import cats.instances.int._
  import cats.instances.string._
  import cats.syntax.show._
  implicit val catShow: Show[Cat] =
    Show.show(cat => s"${cat.name.show} is a ${cat.age.show} year-old ${cat.color.show} cat.")

  import cats.Eq
  import cats.instances.int._
  import cats.instances.string._
  import cats.syntax.eq._
  implicit val catEq: Eq[Cat] =
    Eq.instance[Cat]((c1,c2) => c1.name === c2.name && c1.age === c2.age && c1.color === c2.color)
}
