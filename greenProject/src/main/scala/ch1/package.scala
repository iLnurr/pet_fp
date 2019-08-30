package object ch1 {

  implicit class PrintHelper(val t: String) extends AnyVal {
    def println() = Predef.println(t)
    def print() = Predef.print(t)
  }

}
