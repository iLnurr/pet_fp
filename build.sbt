import Deps._
import Settings._

name := "pet_fp"

version := "0.1"

resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"

lazy val root = project
  .in(file("."))
  .aggregate(redProject, greenProject, blackProject)

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

lazy val blackProject = project
  .in(file("blackProject"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "fs2WSserver",
    libraryDependencies ++= blackProjectDeps
  )

lazy val anService = project
  .in(file("anService"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "an",
    libraryDependencies ++= anServiceDeps
  )