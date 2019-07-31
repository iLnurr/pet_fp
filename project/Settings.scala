import sbt.Keys._
import sbt._

object Settings {
  private val scalacOpts = Seq(
    "-Xfatal-warnings",
    "-Ypartial-unification"
  )
  
  def commonSettings() = Seq(
    scalaVersion := "2.13.0",
    scalacOptions ++= scalacOpts
  )
}
