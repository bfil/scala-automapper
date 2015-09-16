import sbt._
import Keys._
import com.bfil.sbt._

object ProjectBuild extends BFilBuild {

  lazy val root = BFilRootProject("root", file("."))
    .aggregate(automapperMacros, automapper)

  lazy val automapperMacros = BFilProject("automapper-macros", file("automapper-macros"))
    .settings(libraryDependencies ++= Dependencies.scalaReflect(scalaVersion.value))

  lazy val automapper = BFilProject("automapper", file("automapper"))
    .settings(libraryDependencies ++= Dependencies.all)
    .dependsOn(automapperMacros)
}

object Dependencies {

  def scalaReflect(scalaVersion: String) = Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion)

  val all = Seq(
    "org.specs2" %% "specs2-core" % "2.4.17" % "test",
    "org.specs2" %% "specs2-mock" % "2.4.17" % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test")

  val none = Seq.empty

}