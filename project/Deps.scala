import sbt._
object Deps {
  private object Versions {
    val cats = "2.0.0-M4"
    val fs2_http = "0.4.0"
  }

  private lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.cats
  )

  private lazy val fs2 = Seq(
    "com.spinoco" %% "fs2-http" % Versions.fs2_http
  )

  lazy val redProjectDeps: Seq[ModuleID]  = Seq()

  lazy val greenProjectDeps: Seq[ModuleID] = cats

  lazy val blackProjectDeps: Seq[ModuleID] = fs2 ++ cats
}
