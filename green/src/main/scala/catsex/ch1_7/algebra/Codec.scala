package catsex.ch1_7.algebra

trait Codec[A] {
  self =>
  def encode(value: A): String
  def decode(value: String): A
  def imap[B](dec:  A => B, enc: B => A): Codec[B] = new Codec[B] {
    override def encode(value: B): String =
      self.encode(enc(value))
    override def decode(value: String): B =
      dec(self.decode(value))
  }
}
object Codec {
  def encode[A](value: A)(implicit c: Codec[A]): String =
    c.encode(value)
  def decode[A](value: String)(implicit c: Codec[A]): A =
    c.decode(value)
}
object CodecSyntax {
  implicit class CodecOps[A](val a: A) extends AnyVal {
    def encode(implicit c: Codec[A]): String =
      c.encode(a)
  }

  implicit class StringCodecOps[A](val s: String) extends AnyVal {
    def decode(implicit c: Codec[A]): A =
      c.decode(s)
  }
}
