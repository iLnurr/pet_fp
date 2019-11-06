package com.iserba.fp.tagless

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

import com.iserba.fp.{RequestResponseChannel, _}
import com.iserba.fp.model.{Request, Response}
import com.iserba.fp.tagless.algebra.{ClientAlg, ConnectionAlg, ServerAlg}

import scala.collection.mutable
import scala.concurrent.Future

object algebra {
  trait ServerAlg[F[_]] {
    def runServer()(implicit conn: ConnectionAlg[F]): F[(UUID, RequestResponseChannel)]
  }

  trait ConnectionAlg[F[_]] {
    def registerServer(): F[(UUID, RequestResponseChannel)]
    def registerClient(serverId: UUID): F[RequestResponseChannel]
    def createClientConnection(serverId: UUID, clientId: UUID): F[RequestResponseChannel]
  }

  trait ClientAlg[F[_]] {
    def runClient(serverId: UUID)(implicit conn: ConnectionAlg[F]): F[Unit]
    def makeRequest(r: Request)(implicit conn: ConnectionAlg[F]): F[Option[Response]]
  }
}

object impl {
  import com.iserba.fp.utils.StreamProcessHelper._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val connectionImpl = new ConnectionAlg[Future] {
    private val serverConnections = mutable.Map[UUID, RequestResponseChannel]()
    override def createClientConnection(serverId: UUID, clientId: UUID): ParF[RequestResponseChannel] =
      Future.successful(serverConnections(serverId))

    override def registerServer(): ParF[(UUID, RequestResponseChannel)] = {
      val id = UUID.randomUUID()
      val channelToServer = createServerChannel(dummyLogic)
      serverConnections.update(id, channelToServer)
      Future.successful(id -> channelToServer)
    }

    override def registerClient(serverId: UUID): ParF[RequestResponseChannel] =
      Future.successful(serverConnections(serverId))
  }
  val serverImpl = new ServerAlg[Future]{
    override def runServer()(implicit conn: ConnectionAlg[ParF]): ParF[(UUID, RequestResponseChannel)] =
      conn.registerServer()
  }
  val clientImpl = new ClientAlg[Future]{
    private val channelRef = new AtomicReference[Option[RequestResponseChannel]](None)
    override def runClient(serverId: UUID)(implicit conn: ConnectionAlg[ParF]): ParF[Unit] =
      conn.registerClient(serverId).map{channel =>
        channelRef.set(Some(channel))
        channel
      }.map(_ => ())

    override def makeRequest(r: Request)(implicit conn: ConnectionAlg[ParF]): ParF[Option[Response]] = {
      Future {
        channelRef.get().map { channel =>
          val io = emit(r).through(channel).runStreamProcess
          parFIO.unsafePerformIO(io).head
        }
      }
    }
  }
}