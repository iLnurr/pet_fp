package fs2ws.impl.doobie

import cats.effect.{Async, ContextShift}
import cats.syntax.applicativeError._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import fs2.Stream
import fs2ws.Conf

class DoobieService[F[_]: Async: ContextShift: Conf] {
  def startTransactor(
    driver: String = Conf[F].dbDriver,
    url:    String = Conf[F].dbUrl,
    user:   String = Conf[F].dbUser,
    pass:   String = Conf[F].dbPass
  ): Aux[F, Unit] =
    Transactor.fromDriverManager[F](
      driver = driver,
      url    = url,
      user   = user,
      pass   = pass
    )

  implicit lazy val xa: Transactor[F] = startTransactor()

  def selectStream[A](query0: Query0[A]): Stream[F, A] =
    query0.stream.transact(xa)

  def upsert(sql: Fragment): F[Either[Throwable, Int]] =
    sql.update.run.attempt.transact(xa)
}
object DoobieService {
  def apply[F[_]](implicit inst: DoobieService[F]): DoobieService[F] = inst
}
