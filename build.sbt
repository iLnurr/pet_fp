import Deps._
import Settings._

name := "pet_fp"

version := "0.1"

resolvers += "Sonatype Public".at(
  "https://oss.sonatype.org/content/groups/public/"
)

lazy val root = project
  .in(file("."))
  .aggregate(red, green, black, an)

lazy val red = project
  .in(file("red"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "redBookPetProject",
    libraryDependencies ++= redProjectDeps
  )

lazy val green = project
  .in(file("green"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "scalaWithCatsPetProject",
    libraryDependencies ++= greenProjectDeps
  )

lazy val black = project
  .in(file("black"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "fs2WSserver",
    libraryDependencies ++= blackProjectDeps
  )

lazy val an = project
  .in(file("an"))
  .settings(commonSettings())
  .settings(
    version := "0.1",
    name    := "an",
    libraryDependencies ++= anServiceDeps
  )
