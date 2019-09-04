package ch1_7.algebra

trait Printable[A] {
  self =>
  def format(a: A): String

  def contramap[B](func: B => A): Printable[B] =
    new Printable[B] {
      def format(value: B): String =
        self.format(func(value))
    }
}
object Printable {
  def format[A](a: A)(implicit p: Printable[A]): String =
    p.format(a)
  def print[A](a: A)(implicit p: Printable[A]): Unit =
    println(p.format(a))
}
object PrintableSyntax {
  implicit class PrintableOps[A](val a: A) extends AnyVal {
    def format(implicit printable: Printable[A]): String =
      Printable.format(a)
    def print(implicit p: Printable[A]): Unit =
      Printable.print(a)
  }
}
