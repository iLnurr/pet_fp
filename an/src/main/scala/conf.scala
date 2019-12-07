import com.typesafe.config.ConfigFactory

object conf {
  lazy val config   = ConfigFactory.load()
  lazy val dbDriver = config.getString("db.driver")
  lazy val dbUrl    = config.getString("db.url")
  lazy val dbUser   = config.getString("db.user")
  lazy val dbPass   = config.getString("db.password")

  lazy val httpPort = config.getInt("http.port")
  lazy val httpHost = config.getInt("http.host")

  lazy val gmailAddr = config.getString("mailer.host")
  lazy val gmailPass = config.getString("mailer.password")
}
