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
}
