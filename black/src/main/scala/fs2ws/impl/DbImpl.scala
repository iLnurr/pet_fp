package fs2ws.impl

import cats.effect.{Async, ContextShift}
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import fs2.Stream
import fs2ws.conf._
import cats.syntax.applicativeError._

class DbImpl[F[_]: Async: ContextShift] {
  def startTransactor(
    driver: String = dbDriver,
    url:    String = dbUrl,
    user:   String = dbUser,
    pass:   String = dbPass
  ): Aux[F, Unit] =
    Transactor.fromDriverManager[F](
      driver = driver,
      url    = url,
      user   = user,
      pass   = pass
    )

  def selectStream[A](query0: Query0[A])(
    implicit xa:              Transactor[F]
  ): Stream[F, A] = query0.stream.transact(xa)

  def upsert(sql: Fragment)(
    implicit xa:  Transactor[F]
  ): F[Either[Throwable, Int]] = sql.update.run.attempt.transact(xa)
}
