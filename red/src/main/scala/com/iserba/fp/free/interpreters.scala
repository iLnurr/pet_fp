package com.iserba.fp.free

import java.util.UUID

import com.iserba.fp._
import com.iserba.fp.free.algebra._
import com.iserba.fp.utils.Free.{~>, Translate}
import com.iserba.fp.utils.{Free, Monad}

object interpreters {
  def serverToConnectionInterpreter: Translate[ServerAlg, Connection] =
    new (ServerAlg ~> Connection) {
      override def apply[A](f: ServerAlg[A]): Connection[A] = f match {
        case RunServer() =>
          registerServer()
        case ServerChannel(id, channel) =>
          connectServer(id, channel)
      }
    }

  class AlgInterpreter[Alg[_]](
    implicit monad: Monad[Free[Alg, *]]
  ) {
    type FreeAlg[A] = Free[Alg, A]
    def translate(
      serverConnections: Map[UUID, RequestResponseChannel] = Map()
    ): Translate[ConnectionAlg, FreeAlg] =
      new (ConnectionAlg ~> FreeAlg) {
        override def apply[A](f: ConnectionAlg[A]): FreeAlg[A] = f match {
          case ClientConnect(serverId) =>
            monad.unit(serverConnections(serverId))
          case RegisterServer(serverId) =>
            monad.unit(ServerChannel(serverId, createServerChannel(dummyLogic)))
          case ServerConnect(serverId, channel) =>
            monad.unit(serverId -> channel)
        }
      }
  }

  def clientToConnectionInterpreter: Translate[ClientAlg, Connection] =
    new (ClientAlg ~> Connection) {
      override def apply[A](f: ClientAlg[A]): Connection[A] = f match {
        case RunClient(serverId) =>
          createClientConnection(serverId)
      }
    }
}
