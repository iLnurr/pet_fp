import sbt._
object Deps {
  private object Versions {
    val cats = "2.0.0-M4"
  }
  
  private lazy val cats = Seq(
    "org.typelevel" %% "cats-core" % Versions.cats,
    "org.typelevel" %% "cats-effect" % Versions.cats 
  )
  
  lazy val redProjectDeps: Seq[ModuleID]  = Seq()

  lazy val greenProjectDeps: Seq[ModuleID] = cats
}
