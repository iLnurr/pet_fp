import sbt._
object Deps {
  private object Versions {
    val cats        = "2.1.1"
    val catsTagless = "0.11"
    val shapeless   = "2.3.3"

    val scalaLogging = "3.9.2"
    val logback      = "1.2.3"

    val circe         = "0.13.0"
    val http4sVersion = "0.21.7"
    val sttp          = "2.0.0-RC5"

    val doobieVersion = "0.9.0"

    val fs2_kafka = "1.0.0"

    val scalaTest             = "3.0.8"
    val testcontainers        = "0.34.1"
    val kafkaTestContainer    = "1.12.3"
    val postgresTestcontainer = "1.12.3"
    val mysqlTestcontainer    = "1.12.3"
  }

  private lazy val logging = Seq(
    "com.typesafe.scala-logging" %% "scala-logging"  % Versions.scalaLogging,
    "ch.qos.logback"             % "logback-core"    % Versions.logback,
    "ch.qos.logback"             % "logback-classic" % Versions.logback
  )

  private lazy val testDeps = Seq(
    "org.scalatest"                %% "scalatest"                      % Versions.scalaTest,
    "com.dimafeng"                 %% "testcontainers-scala"           % Versions.testcontainers,
    "com.dimafeng"                 %% "testcontainers-scala-scalatest" % Versions.testcontainers,
    "org.testcontainers"           % "postgresql"                      % Versions.postgresTestcontainer,
    "org.testcontainers"           % "kafka"                           % Versions.kafkaTestContainer,
    "org.testcontainers"           % "mysql"                           % Versions.mysqlTestcontainer,
    "com.softwaremill.sttp.client" %% "async-http-client-backend-fs2"  % Versions.sttp // test with websocket client
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

  private lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core"      % Versions.doobieVersion,
    "org.tpolecat" %% "doobie-h2"        % Versions.doobieVersion,
    "org.tpolecat" %% "doobie-hikari"    % Versions.doobieVersion, // HikariCP transactor.
    "org.tpolecat" %% "doobie-postgres"  % Versions.doobieVersion, // Postgres driver 42.2.8 + type mappings
    "org.tpolecat" %% "doobie-scalatest" % Versions.doobieVersion % "test" // ScalaTest support for typechecking statements.
  )

  private lazy val fs2_kafka = Seq(
    "com.github.fd4s" %% "fs2-kafka" % Versions.fs2_kafka
  )

  lazy val redProjectDeps: Seq[ModuleID] = Seq()

  lazy val greenProjectDeps
    : Seq[ModuleID] = cats ++ catsEffect ++ catsTagless ++ conf ++ shapeless

  lazy val blackProjectDeps
    : Seq[ModuleID] = http4s ++ cats ++ catsEffect ++ catsTagless ++ circe ++ conf ++ doobie ++ fs2_kafka ++ logging ++ testDeps
}
