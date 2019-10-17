import sbt._
object Deps {
  private object Versions {
    val cats = "2.0.0-M4"
    val fs2_http = "0.4.0"
    val circe = "0.11.1"
    val enumeratum = "1.5.13"
    val catsTagless  = "0.9"
    val shapeless = "2.3.3"
  }

  private lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % Versions.cats,
    "org.typelevel" %% "cats-free" % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.cats
  )

  private lazy val catsEffect = Seq("org.typelevel" %% "cats-effect" % Versions.cats)
  private lazy val catsTagless = Seq("org.typelevel" %% "cats-tagless-macros" % Versions.catsTagless)

  private lazy val fs2 = Seq(
    "com.spinoco" %% "fs2-http" % Versions.fs2_http
  )

  private lazy val enumeratum = Seq(
    "com.beachape" %% "enumeratum" % Versions.enumeratum,
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

  lazy val redProjectDeps: Seq[ModuleID]  = Seq()

  lazy val greenProjectDeps: Seq[ModuleID] = cats ++ catsEffect ++ catsTagless ++ conf ++ shapeless

  lazy val blackProjectDeps: Seq[ModuleID] = fs2 ++ cats ++ catsEffect ++ catsTagless ++ circe ++ enumeratum ++ conf
}
