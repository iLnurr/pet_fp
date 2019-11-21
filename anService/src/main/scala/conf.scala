import com.typesafe.config.ConfigFactory

object conf {
  val config   = ConfigFactory.load()
  val dbDriver = config.getString("db.driver")
  val dbUrl    = config.getString("db.url")
  val dbUser   = config.getString("db.user")
  val dbPass   = config.getString("db.password")
}
