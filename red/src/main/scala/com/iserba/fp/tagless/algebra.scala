package com.iserba.fp.tagless

import java.util.UUID

import com.iserba.fp.RequestResponseChannel
import com.iserba.fp.model.{Request, Response}

object algebra {
  type ChannelOnDemand = () => RequestResponseChannel
  trait ServerAlg[F[_]] {
    def runServer()(implicit conn: ConnectionAlg[F]): F[(UUID, ChannelOnDemand)]
  }

  trait ConnectionAlg[F[_]] {
    def registerServer(): F[(UUID, ChannelOnDemand)]
    def registerClient(serverId: UUID): F[RequestResponseChannel]
  }

  trait ClientAlg[F[_]] {
    def runClient(serverId: UUID)(implicit conn: ConnectionAlg[F]): F[Unit]
    def makeRequest(r:      Request)(
      implicit conn:        ConnectionAlg[F]
    ): F[Option[Response]]
  }
}
