import cats.effect._
import org.http4s.{HttpRoutes, QueryParamDecoder}
import org.http4s.syntax._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._
import cats.effect._
import com.typesafe.scalalogging.Logger
import db.Data
import doobie.util.transactor.Transactor
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

object http {
  private val logger = Logger("http")
  implicit private val decoder: EntityDecoder[IO, GetInfo] = jsonOf[IO, GetInfo]
  private def route(implicit xa: Transactor[IO]) =
    HttpRoutes
      .of[IO] {
        case req @ POST ->
            Root / "records" / "body" / "mail" / mail =>
          for {
            info    <- req.as[GetInfo]
            records <- processGetRequest(info)
            _       <- sendToMail(mail, records)
            resp    <- Ok(records.mkString(",").asJson)
          } yield (resp)
      }
      .orNotFound

  def startHttpServer()(
    implicit cs: ContextShift[IO],
    timer:       Timer[IO],
    xa:          Transactor[IO]
  ): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(conf.httpPort, "localhost")
      .withHttpApp(route)
      .serve
      .compile
      .drain
      .map(_ => ExitCode.Success)

  private def processGetRequest(getInfo: GetInfo)(implicit xa: Transactor[IO]) =
    db.getRecords(getInfo).map(_._2)

  private def sendToMail(mail: String, records: Data) = IO.pure {
    logger.debug(s"Send records to mail: $mail. \n Records: $records")
  }
}
