import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp}
import com.typesafe.scalalogging.Logger

object main extends IOApp {
  val logger = Logger("an_service")
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  override def run(args: List[String]): IO[ExitCode] = {
    logger.info(s"starting service")
    IO(ExitCode.Success)
  }

}
