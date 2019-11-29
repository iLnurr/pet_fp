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
import model.{ClientInfo, GetInfo, HouseInfo, QueryInfo}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import db._

import scala.concurrent.{ExecutionContext, Future}

object http {
  private val logger = Logger("http")
  implicit private val decoderGI: EntityDecoder[IO, GetInfo] =
    jsonOf[IO, GetInfo]
  implicit private val decoderQI: EntityDecoder[IO, QueryInfo] =
    jsonOf[IO, QueryInfo]
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
            records <- db.getRecords(db.constructQuery(info)).map(_._2)
            _ <- sendToMail(
              mail,
              "search result by quiz from tradegoria",
              "sorry but right now we not found anything",
              records
            )
            resp <- Ok(records.mkString(",").asJson)
          } yield (resp)
        case req @ POST ->
            Root / "records" / "body" / "query" =>
          for {
            info    <- req.as[QueryInfo]
            _       <- IO(logger.debug(s"Got request, info=$info"))
            records <- db.getRecords(db.constructQuery(info)).map(_._2)
            _ <- sendToMail(
              info.client.mail,
              "search result by quiz from tradegoria",
              "sorry but right now we not found anything",
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

  private def sendToMail(
    mail:            String,
    subject:         String,
    subjectNotFound: String,
    records:         Data
  )(
    implicit cs: ContextShift[IO]
  ) = IO.fromFuture(
    IO {
      if (records.nonEmpty) {
        logger.debug(s"Send records to mail: $mail. \n Records: $records")
        mailer.send(subject, records.mkString(","), mail)
      } else {
        logger.debug(s"Not found anything, send info to mail: $mail.")
        mailer
          .send(
            subjectNotFound,
            "But you can look another houses: https://tradegoria.com",
            mail
          )
      }
    }
  )
}
