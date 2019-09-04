package object ch1_7 {

  implicit class PrintHelper[T](val t: T) extends AnyVal {
    def println() = Predef.println(t.toString)
  }

}
