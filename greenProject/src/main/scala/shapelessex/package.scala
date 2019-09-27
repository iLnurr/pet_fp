package object shapelessex {
  implicit class PrintHelper[T](val t: T) extends AnyVal {
    def tapPrintln(): T = {
      Predef.println(t.toString)
      t
    }
  }
}
