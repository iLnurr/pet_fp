package com.iserba

import com.iserba.fp.model.Request
import com.iserba.fp.model.Response
import com.iserba.fp.utils.StreamProcess.Channel
import com.iserba.fp.utils.StreamProcessHelper.{constantF, emit}
import com.iserba.fp.utils.{Free, IOWrap, Monad, StreamProcess}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

package object fp {
  type RequestStream           = StreamProcess[IO, Request]
  type ResponseStream          = StreamProcess[IO, Response]
  type ResponseStreamByRequest = Request => ResponseStream
  type RequestResponseChannel  = Channel[IO, Request, Response] //StreamProcess[IO, ResponseStreamByRequest]

  def createServerChannel(
    converter: Converter[Request, Response]
  ): RequestResponseChannel =
    constantF[IO, ResponseStreamByRequest](req => emit(converter.convert(req)))
  val dummyLogic: Converter[Request, Response] =
    new Converter[Request, Response] {
      def convert: Request => Response =
        req => Response(req.entity.map(ev => ev.copy(ts = ev.ts * 2)))
    }

  type ParF[+A] = Future[A]
  implicit val monadImpl: Monad[ParF] = instances.FutureM.monadFuture
  implicit val monadCatchImpl: Monad.MonadCatch[ParF] =
    instances.FutureM.monadCatchFuture
  val parFIO: IOWrap[ParF] = IOWrap[ParF](instances.FutureM.parFuture)
  implicit val ioMC: Monad.MonadCatch[Free[ParF, *]] =
    parFIO.ioMC

  type IO[A] = parFIO.IO[A] // Free[ParF,A]
  def IO[A](a: => A): IO[A] =
    parFIO.IO(a)
}
