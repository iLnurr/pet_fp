package shapelessex.ch3

trait MyTC[A] {
  def to(a: A): Array[Byte]

  def from(b: Array[Byte]): A
}

object MyTC {
  def mat[A](implicit inst: MyTC[A]): MyTC[A] =
    inst

  def pure[A](toF: A => Array[Byte])(fromF: Array[Byte] => A): MyTC[A] =
    new MyTC[A] {
      def to(a: A): Array[Byte] =
        toF(a)

      def from(b: Array[Byte]): A =
        fromF(b)
    }
}
