import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import com.typesafe.scalalogging.Logger
import cats.syntax.flatMap._

object main extends IOApp {
  val logger = Logger("an_service")
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] = {
    logger.info(s"starting service")
    implicit val xa = db.startTransactor()
    http.startHttpServer() >> IO(
      ExitCode.Success
    )
  }

}
