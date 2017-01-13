import sbt._
import Keys._
import com.bfil.sbt._

object ProjectBuild extends BFilBuild {

  lazy val root = BFilRootProject("root", file("."))
    .settings(crossScalaVersions := Seq("2.12.1", "2.11.8"))
    .aggregate(automapperMacros, automapper)

  lazy val automapperMacros = BFilProject("automapper-macros", file("automapper-macros"))
    .settings(libraryDependencies ++= Dependencies.scalaReflect(scalaVersion.value))

  lazy val automapper = BFilProject("automapper", file("automapper"))
    .settings(libraryDependencies ++= Dependencies.scalatest)
    .dependsOn(automapperMacros)
}

object Dependencies {

  def scalaReflect(scalaVersion: String) = Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion
  )

  val scalatest = Seq(
    "org.scalatest" %% "scalatest" % "3.0.1"
  )

}
