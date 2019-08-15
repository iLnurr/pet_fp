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
    )
  )
}
