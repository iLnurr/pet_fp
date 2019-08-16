package fs2ws

import fs2ws.Domain.User

object DBTest extends App {
  val users = Users

  (for {
    _ <- users.list.map(list => assert(list.size == 2, "2"))
    _ <- users.add(User(id = Some(3),name = "dd", password = "pp", user_type = "user"))
    _ <- users.list.map(seq => assert(seq.size == 3, "3"))
  } yield ()).unsafeRunSync()

}
