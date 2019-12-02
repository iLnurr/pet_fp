package com.iserba.fp.tagless

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

import com.iserba.fp.model.{Request, Response}
import com.iserba.fp.{RequestResponseChannel, _}
import com.iserba.fp.tagless.algebra.{
  ChannelOnDemand,
  ClientAlg,
  ConnectionAlg,
  ServerAlg
}

import scala.collection.mutable
import scala.concurrent.Future

object impl {
  import com.iserba.fp.utils.StreamProcessHelper._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val connectionImpl = new ConnectionAlg[Future] {
    private val serverConnections = mutable.Map[UUID, ChannelOnDemand]()

    override def registerServer(): ParF[(UUID, ChannelOnDemand)] = {
      val id              = UUID.randomUUID()
      val channelToServer = () => createServerChannel(dummyLogic)
      serverConnections.update(id, channelToServer)
      Future.successful(id -> channelToServer)
    }

    override def registerClient(serverId: UUID): ParF[RequestResponseChannel] =
      Future.successful(serverConnections(serverId)())
  }
  val serverImpl = new ServerAlg[Future] {
    override def runServer()(
      implicit conn: ConnectionAlg[ParF]
    ): ParF[(UUID, ChannelOnDemand)] =
      conn.registerServer()
  }
  val clientImpl = new ClientAlg[Future] {
    private val channelRef =
      new AtomicReference[Option[RequestResponseChannel]](None)
    override def runClient(
      serverId:      UUID
    )(implicit conn: ConnectionAlg[ParF]): ParF[Unit] =
      conn.registerClient(serverId).map { channel =>
        channelRef.set(Some(channel))
      }

    override def makeRequest(
      r:             Request
    )(implicit conn: ConnectionAlg[ParF]): ParF[Option[Response]] =
      Future {
        channelRef.get().map { channel =>
          val io = emit(r).through(channel).runStreamProcess
          parFIO.unsafePerformIO(io).head
        }
      }
  }
}
