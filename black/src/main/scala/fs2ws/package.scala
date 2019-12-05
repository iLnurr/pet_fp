import fs2.Pipe
import fs2ws.Domain.{Message, Table, User}

package object fs2ws {
  type MsgStreamPipe[F[_]] = Pipe[F, Message, Message]

  type UserReader[F[_]]  = DbReaderAlgebra[F, User]
  type UserWriter[F[_]]  = DbWriterAlgebra[F, User]
  type TableReader[F[_]] = DbReaderAlgebra[F, Table]
  type TableWriter[F[_]] = DbWriterAlgebra[F, Table]
}
