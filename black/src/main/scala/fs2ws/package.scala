import fs2.Pipe
import fs2ws.Domain.{Message, Table, User}

package object fs2ws {
  type MsgStreamPipe[F[_]] = Pipe[F, Message, Message]

  type UserReader[F[_]]  = DbReader[F, User]
  type UserWriter[F[_]]  = DbWriter[F, User]
  type TableReader[F[_]] = DbReader[F, Table]
  type TableWriter[F[_]] = DbWriter[F, Table]
}
