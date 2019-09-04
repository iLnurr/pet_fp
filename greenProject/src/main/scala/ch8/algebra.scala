package ch8

import cats.instances.list._
import cats.syntax.traverse._
import cats.{Applicative, Functor}

trait UptimeClient[F[_]] {
  def getUptime(hostname: String): F[Int]
}
abstract class UptimeService[F[_]: Applicative] {
  def client: UptimeClient[F]
  def getTotalUptime(hostnames: List[String]): F[Int] = {
    Functor[F].map(hostnames.traverse(client.getUptime))(_.sum)
  }
}
