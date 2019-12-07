package an

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
import model.{ClientInfo, GetInfo, HouseInfo, QueryInfo, TradSearchResult}
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import db._
import custom.queries._

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
  private def route(implicit xa: Transactor[IO]) = {
    implicit val cs: ContextShift[IO] = blockingCS
    HttpRoutes
      .of[IO] {
        case req @ POST ->
            Root / "records" / "body" / "mail" / mail =>
          for {
            info    <- req.as[GetInfo]
            records <- db.getRecords(constructQuery(info)).map(_._2)
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
            info <- req.as[QueryInfo]
            _ <- IO(
              logger
                .debug(
                  s"Got request \nbody=${req.as[String].unsafeRunSync()}, \ninfo=$info"
                )
            )
            query <- IO.pure {
              val q = constructQuery(info)
              logger.debug(s"Query \n$q\n")
              q
            }
            records <- db
              .getRecords(query)
              .map(_._2)
              .handleErrorWith { er =>
                IO.pure {
                  logger.error("DB ERROR", er)
                  List()
                }
              }
            _ <- sendToMail(
              info.client.mail,
              "search result by quiz from tradegoria.com",
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
        logger.debug(
          s"Send records to mail: $mail. \n Records size: ${records.size}"
        )

        mailer.send(
          subject = subject,
          content = TradSearchResult.htmlTable(records),
          isHtml  = true,
          to      = mail
        )
      } else {
        logger.debug(s"Not found, send info to mail: $mail.")
        mailer
          .send(
            subject = subjectNotFound,
            content = "But you can look another houses: https://tradegoria.com",
            isHtml  = false,
            to      = mail
          )
      }
    }
  )
}
