import sbt.Keys._
import sbt._

object Settings {
  def commonSettings() = Seq(
    scalaVersion := "2.13.0"
  )
}
