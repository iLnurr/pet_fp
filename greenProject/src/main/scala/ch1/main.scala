package ch1

import ch1.domain.Cat
import impl.catPrintable
import PrintableSyntax._

object main extends App {
  import Printable._
  val cat = Cat("c", 1, "red")

  print(cat)

  cat.print
}
