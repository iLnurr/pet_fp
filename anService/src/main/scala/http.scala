import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.Executors

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

import scala.concurrent.ExecutionContext

object http {
  private val logger = Logger("http")
  implicit private val decoder: EntityDecoder[IO, GetInfo] = jsonOf[IO, GetInfo]
  private val blockingEC =
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val blockingCS: ContextShift[IO] =
    IO.contextShift(blockingEC)
  lazy val file = Files
    .write(
      Paths.get(new File("quiz.html").getAbsolutePath),
      quiz.html.getBytes(java.nio.charset.Charset.forName("CP1251"))
    )
    .toFile
  private def route(implicit xa: Transactor[IO]) = {
    implicit val cs: ContextShift[IO] = blockingCS
    HttpRoutes
      .of[IO] {
        case request @ GET -> Root / "quiz.html" =>
          StaticFile
            .fromFile(
              file,
              blockingEC,
              Some(request)
            )
            .getOrElseF(NotFound())
        case req @ POST ->
            Root / "records" / "body" / "mail" / mail =>
          for {
            info    <- req.as[GetInfo]
            records <- processGetRequest(info)
            _ <- sendToMail(
              mail,
              "search result by quiz from tradegoria",
              records
            )
            resp <- Ok(records.mkString(",").asJson)
          } yield (resp)
      }
      .orNotFound
  }

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

  private def sendToMail(mail: String, subject: String, records: Data)(
    implicit cs:               ContextShift[IO]
  ) = IO.fromFuture(
    IO {
      logger.debug(s"Send records to mail: $mail. \n Records: $records")
      mailer.send(subject, records.mkString(","), mail)
    }
  )
}
