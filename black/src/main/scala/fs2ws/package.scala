import fs2.Pipe
import fs2ws.Domain.{Message, Table, User}

package object fs2ws {
  type MsgStreamPipe[F[_]] = Pipe[F, Message, Message]
}
