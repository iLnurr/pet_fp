package fs2ws

import cats.tagless.finalAlg

@finalAlg
trait MessageProcessorAlgebra[F[_],I,O] {
  def handler: I => F[O]
}

@finalAlg
trait JsonEncoder[F[_], A] {
  def toJson(value: A): F[String]
}

@finalAlg
trait JsonDecoder[F[_], A] {
  def fromJson(json: String): F[A]
}

@finalAlg
trait ServerAlgebra[F[_],I,O] {
  def processEvent(raw: String)
                       (implicit
                          decoder: JsonDecoder[F, I],
                          encoder: JsonEncoder[F,O],
                          processor: MessageProcessorAlgebra[F,I,O]): F[String]
  def startWS(port: Int): F[Unit]
}
