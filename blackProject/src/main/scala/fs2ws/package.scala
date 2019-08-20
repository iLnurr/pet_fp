import cats.effect.IO
import fs2.Pipe
import spinoco.fs2.http.websocket.Frame

package object fs2ws {
  type FS2StreamPipe = Pipe[IO,Frame[String], Frame[String]]
}
