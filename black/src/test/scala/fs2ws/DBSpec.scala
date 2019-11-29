package fs2ws

import cats.effect.IO
import fs2ws.Domain.User
import fs2ws.impl.InMemoryDB
import org.scalatest.{FlatSpec, Matchers}

class DBSpec extends FlatSpec with Matchers {
  behavior.of("StorageReader/StorageWriter")

  private val userReader: UserReader[IO] = InMemoryDB.Users
  private val userWriter: UserWriter[IO] = InMemoryDB.Users
  it should "properly get list, add, update, remove entities" in {
    (for {
      _ <- userReader.list.map(l => l.size shouldBe 2)
      _ <- userWriter.add(
        0,
        User(id = Some(2), name = "dd", password = "pp", user_type = "user")
      )
      _ <- userWriter.add(
        1,
        User(id = Some(3), name = "ddd", password = "pp", user_type = "user")
      )
      _ <- userWriter.add(
        2,
        User(id = Some(4), name = "dddd", password = "pp", user_type = "user")
      )
      _ <- userWriter.add(
        -1,
        User(id = Some(5), name = "aa", password = "pp", user_type = "user")
      )
      _ <- userWriter.add(
        1,
        User(id = Some(6), name = "ss", password = "pp", user_type = "user")
      )
      _ <- userReader.list.map(l => l.size shouldBe 7)
      _ <- userWriter.update(
        User(id = Some(3), name = "dd_u", password = "pp_U", user_type = "user")
      )
      _ <- userReader.list.map(l => l.size shouldBe 7)
      _ <- userWriter.remove(3)
      _ <- userReader.list.map(l => l.size shouldBe 6)
    } yield ()).unsafeRunSync()
  }
}
