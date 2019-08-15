import sbt._
object Deps {
  private object Versions {
    val cats = "2.0.0-M4"
    val fs2_http = "0.4.0"
    val circe = "0.11.1"
    val enumeratum = "1.5.13"
  }

  private lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.cats
  )

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
    "io.circe" %% "circe-refined"
  ).map(_ % Versions.circe)

  lazy val redProjectDeps: Seq[ModuleID]  = Seq()

  lazy val greenProjectDeps: Seq[ModuleID] = cats

  lazy val blackProjectDeps: Seq[ModuleID] = fs2 ++ cats ++ circe ++ enumeratum
}
