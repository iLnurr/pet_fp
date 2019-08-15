import sbt.Keys._
import sbt._

object Settings {
  def commonSettings() = Seq(
    scalaVersion := "2.12.8",
    Compile/scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-language:higherKinds",
      "-language:existentials"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )
}
