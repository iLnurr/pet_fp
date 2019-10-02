package shapelessex.ch3

import java.nio.charset.StandardCharsets

import shapeless._

object impl {

  implicit def intInstance: MyTC[Int] =
    MyTC.pure[Int](BigInt(_).toByteArray)(BigInt(_).toInt)

  implicit def stringInstance: MyTC[String] =
    MyTC.pure[String](_.getBytes(StandardCharsets.UTF_8))(new String(_, StandardCharsets.UTF_8))

  implicit def booleanInstance: MyTC[Boolean] =
    MyTC.pure[Boolean](b => BigInt(if (b) 1 else 0).toByteArray)(BigInt(_).toInt == 1)

  implicit def hnilInstance: MyTC[HNil] =
    MyTC.pure[HNil](_ => Array.emptyByteArray)(_ => HNil)

  implicit def hlistInstance[H, T <: HList](implicit
                                            hInstance: Lazy[MyTC[H]], // wrap in Lazy
                                            tInstance: MyTC[T]
                                           ): MyTC[H :: T] =
    MyTC.pure[H :: T] {
      case h :: t =>
        hInstance.value.to(h) ++ tInstance.to(t)
    } { bytes =>
      hInstance.value.from(bytes) :: tInstance.from(bytes)
    }

  implicit def cnilInstance: MyTC[CNil] =
    MyTC.pure[CNil](_ => throw new RuntimeException("Inconceviable c"))(_ => throw new RuntimeException("Inconceviable b"))

  implicit def coproductInstance[H, T <: Coproduct](implicit
                                                    hInstance: Lazy[MyTC[H]], // wrap in Lazy
                                                    tInstance: MyTC[T]
                                                   ): MyTC[H :+: T] =
    MyTC.pure[H :+: T] {
      case Inl(head) =>
        hInstance.value.to(head)
      case Inr(tail) =>
        tInstance.to(tail)
    } { bytes =>
      Inl(hInstance.value.from(bytes)) // Inr(tInstance.from(bytes))
    }

  implicit def genericInstance[A, R](
                                      implicit
                                      generic: Generic.Aux[A, R],
                                      rInstance: Lazy[MyTC[R]] // wrap in Lazy
                                    ): MyTC[A] =
    MyTC.pure[A](a => rInstance.value.to(generic.to(a)))(bytes => generic.from(rInstance.value.from(bytes)))

}
