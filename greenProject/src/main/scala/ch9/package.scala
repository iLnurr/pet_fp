package object ch9 {

  implicit class PrintHelper[T](val t: T) extends AnyVal {
    def println() = Predef.println(t.toString)
  }

}
