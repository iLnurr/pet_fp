import courier._
import Defaults._
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

object mailer {
  private val logger = Logger("mailer")

  lazy val mailerHost = conf.gmailAddr
  lazy val password   = conf.gmailPass
  private val port    = 587
  private lazy val mailer = Mailer("smtp.gmail.com", port)
    .auth(true)
    .as(mailerHost, password)
    .startTls(true)()

  def send(
    subject: String,
    text:    String,
    to:      String,
    from:    String = mailerHost,
    cc:      Seq[String] = Seq()
  ): Future[Unit] = {
    val envelop = Envelope
      .from(from.addr)
      .to(to.addr)
      .subject(subject)
      .content(Text(text))
      .cc(cc.map(_.addr): _*)

    mailer(envelop).map(
      _ =>
        logger.debug(s"Mail from=$from to=$to cc=${cc.mkString(",")} delivered")
    )
  }
}
