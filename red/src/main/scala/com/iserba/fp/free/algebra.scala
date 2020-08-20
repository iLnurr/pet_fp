package com.iserba.fp.free

import java.util.UUID

import com.iserba.fp._
import com.iserba.fp.utils.Free

object algebra {
  sealed trait ServerAlg[T]
  case class RunServer() extends ServerAlg[ServerChannel]
  case class ServerChannel(id: UUID, channel: RequestResponseChannel)
      extends ServerAlg[(UUID, RequestResponseChannel)]
  type Server[T] = Free[ServerAlg, T]
  def runServer(): Server[ServerChannel] =
    Free.liftF(RunServer())
  def serverChannel(id: UUID): Server[(UUID, RequestResponseChannel)] =
    Free.liftF(ServerChannel(id, createServerChannel(dummyLogic)))

  sealed trait ConnectionAlg[T]
  case class ClientConnect(serverId: UUID)
      extends ConnectionAlg[RequestResponseChannel]
  case class RegisterServer(serverId: UUID) extends ConnectionAlg[ServerChannel]
  case class ServerConnect(serverId:  UUID, channel: RequestResponseChannel)
      extends ConnectionAlg[(UUID, RequestResponseChannel)]
  type Connection[T] = Free[ConnectionAlg, T]
  def createClientConnection(
    serverId: UUID
  ): Connection[RequestResponseChannel] =
    Free.liftF(ClientConnect(serverId))
  def registerServer(): Connection[ServerChannel] =
    Free.liftF(RegisterServer(UUID.randomUUID()))
  def connectServer(
    serverId: UUID,
    channel:  RequestResponseChannel
  ): Connection[(UUID, RequestResponseChannel)] =
    Free.liftF(ServerConnect(serverId, channel))

  sealed trait ClientAlg[T]
  case class RunClient(serverId: UUID) extends ClientAlg[RequestResponseChannel]
  type Client[T] = Free[ClientAlg, T]
  def runClient(serverId: UUID): Client[RequestResponseChannel] =
    Free.liftF(RunClient(serverId))
}
