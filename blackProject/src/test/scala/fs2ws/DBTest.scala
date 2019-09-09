package fs2ws

import fs2ws.Domain.User

object DBTest extends App {
  val users = Users

  (for {
    _ <- users.list.map(l => assert(l.size == 2, "2"))
    _ <- users.add(0, User(id = Some(0),name = "dd", password = "pp", user_type = "user"))
    _ <- users.add(1, User(id = Some(1),name = "ddd", password = "pp", user_type = "user"))
    _ <- users.add(2, User(id = Some(2),name = "dddd", password = "pp", user_type = "user"))
    _ <- users.add(-1, User(id = Some(2),name = "aa", password = "pp", user_type = "user"))
    _ <- users.add(1, User(id = Some(2),name = "ss", password = "pp", user_type = "user"))
//    _ <- users.list.map(l => assert(l.size == 3, "3"))
    _ <- users.update(User(id = Some(3),name = "dd_u", password = "pp_U", user_type = "user"))
//    _ <- users.list.map(l => assert(l.size == 3, "3"))
    _ <- users.remove(3)
//    _ <- users.list.map(l => assert(l.size == 2, "2"))
  } yield ()).unsafeRunSync()

}
