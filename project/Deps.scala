import sbt._
object Deps {
  private object Versions {
    val cats        = "2.0.0"
    val enumeratum  = "1.5.13"
    val catsTagless = "0.9"
    val shapeless   = "2.3.3"

    val scalaLogging = "3.9.2"
    val logback      = "1.2.3"

    val circe         = "0.11.1"
    val fs2_http      = "0.4.0"
    val http4sVersion = "0.20.13"

    val doobieVersion  = "0.8.6"
    val mysqlConnector = "8.0.18"

    val mailer = "2.0.0"

    val scalaTest             = "3.0.8"
    val testcontainers        = "0.33.0"
    val kafkaTestContainer    = "1.12.3"
    val postgresTestcontainer = "1.12.3"
  }

  private lazy val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging"  % Versions.scalaLogging,
    "ch.qos.logback"             % "logback-core"    % Versions.logback,
    "ch.qos.logback"             % "logback-classic" % Versions.logback
  )

  private lazy val testDeps = Seq(
    "org.scalatest"      %% "scalatest"            % Versions.scalaTest,
    "com.dimafeng"       %% "testcontainers-scala" % Versions.testcontainers,
    "org.testcontainers" % "postgresql"            % Versions.postgresTestcontainer,
    "org.testcontainers" % "kafka"                 % Versions.kafkaTestContainer
  ).map(_ % Test)

  private lazy val cats = Seq(
    "org.typelevel" %% "cats-core"   % Versions.cats,
    "org.typelevel" %% "cats-free"   % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.cats
  )

  private lazy val catsEffect = Seq(
    "org.typelevel" %% "cats-effect" % Versions.cats
  )
  private lazy val catsTagless = Seq(
    "org.typelevel" %% "cats-tagless-macros" % Versions.catsTagless
  )

  private lazy val fs2 = Seq(
    "com.spinoco" %% "fs2-http" % Versions.fs2_http
  )

  private lazy val enumeratum = Seq(
    "com.beachape" %% "enumeratum"       % Versions.enumeratum,
    "com.beachape" %% "enumeratum-circe" % Versions.enumeratum
  )

  private lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-refined",
    "io.circe" %% "circe-generic-extras"
  ).map(_ % Versions.circe)

  private lazy val conf = Seq(
    "com.typesafe" % "config" % "1.3.4"
  )

  private lazy val shapeless = Seq(
    "com.chuusai" %% "shapeless" % Versions.shapeless
  )

  private lazy val http4s = Seq(
    "org.http4s" %% "http4s-dsl"          % Versions.http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % Versions.http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % Versions.http4sVersion
  )

  private lazy val http4s_circe = Seq(
    "org.http4s" %% "http4s-circe" % Versions.http4sVersion,
    // Optional for auto-derivation of JSON codecs
    "io.circe" %% "circe-generic" % Versions.circe,
    // Optional for string interpolation to JSON model
    "io.circe" %% "circe-literal" % Versions.circe
  )

  private lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core"      % Versions.doobieVersion,
    "org.tpolecat" %% "doobie-h2"        % Versions.doobieVersion,
    "org.tpolecat" %% "doobie-hikari"    % Versions.doobieVersion, // HikariCP transactor.
    "org.tpolecat" %% "doobie-postgres"  % Versions.doobieVersion, // Postgres driver 42.2.8 + type mappings
    "org.tpolecat" %% "doobie-scalatest" % Versions.doobieVersion % "test" // ScalaTest support for typechecking statements.
  )

  private lazy val mysql = Seq(
    "mysql" % "mysql-connector-java" % Versions.mysqlConnector
  )

  private lazy val mailer = Seq(
    "com.github.daddykotex" %% "courier" % Versions.mailer
  )

  lazy val redProjectDeps: Seq[ModuleID] = Seq()

  lazy val greenProjectDeps
    : Seq[ModuleID] = cats ++ catsEffect ++ catsTagless ++ conf ++ shapeless

  lazy val blackProjectDeps
    : Seq[ModuleID] = fs2 ++ cats ++ catsEffect ++ catsTagless ++ circe ++ conf ++ doobie ++ logging ++ testDeps

  lazy val anServiceDeps: Seq[ModuleID] = cats ++ catsEffect ++ catsTagless ++
    conf ++
    shapeless ++
    http4s ++ http4s_circe ++
    doobie ++ mysql ++
    logging ++
    mailer ++
    testDeps
}
