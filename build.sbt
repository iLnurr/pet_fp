import Deps._
import Settings._

name := "pet_fp"

version := "0.1"

lazy val root = project
  .in(file("."))
  .aggregate(redProject, greenProject)

lazy val redProject = project
  .in(file("redProject"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "redBookPetProject",
    libraryDependencies ++= redProjectDeps
  )

lazy val greenProject = project
  .in(file("greenProject"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "scalaWithCatsPetProject",
    libraryDependencies ++= greenProjectDeps
  )