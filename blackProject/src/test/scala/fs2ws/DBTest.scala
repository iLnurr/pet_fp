package fs2ws

import fs2ws.Domain.User

object DBTest extends App {
  val users = Users

  (for {
    _ <- users.list.map(l => assert(l.size == 2, "2"))
    _ <- users.add(0, User(id = Some(2),name = "dd", password = "pp", user_type = "user"))
    _ <- users.add(1, User(id = Some(3),name = "ddd", password = "pp", user_type = "user"))
    _ <- users.add(2, User(id = Some(4),name = "dddd", password = "pp", user_type = "user"))
    _ <- users.add(-1, User(id = Some(5),name = "aa", password = "pp", user_type = "user"))
    _ <- users.add(1, User(id = Some(6),name = "ss", password = "pp", user_type = "user"))
    _ <- users.list.map(l => assert(l.size == 7, s"${l.size} != 7"))
    _ <- users.update(User(id = Some(3),name = "dd_u", password = "pp_U", user_type = "user"))
    _ <- users.list.map(l => assert(l.size == 7, s"${l.size} != 7"))
    _ <- users.remove(3)
    _ <- users.list.map(l => assert(l.size == 6, "6"))
  } yield ()).unsafeRunSync()

}
