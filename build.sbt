lazy val root = Project("root", file("."))
  .settings(settings, publishArtifact := false)
  .aggregate(automapperMacros, automapper)

lazy val automapperMacros = Project("automapper-macros", file("automapper-macros"))
  .settings(settings, libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value
  ))

lazy val automapper = Project("automapper", file("automapper"))
  .settings(settings, libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.8" % "test"
  ))
  .dependsOn(automapperMacros)

lazy val settings = Seq(
  scalaVersion := "2.13.0",
  crossScalaVersions := Seq("2.13.0"),
  organization := "io.bfil",
  organizationName := "Bruno Filippone",
  organizationHomepage := Some(url("http://bfil.io")),
  homepage := Some(url("https://github.com/bfil/scala-automapper")),
  licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
  developers := List(
    Developer("bfil", "Bruno Filippone", "bruno@bfil.io", url("http://bfil.io"))
  ),
  startYear := Some(2015),
  publishTo := Some("Bintray" at s"https://api.bintray.com/maven/bfil/maven/${name.value}"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".bintray-credentials"),
  scmInfo := Some(ScmInfo(
    url(s"https://github.com/bfil/scala-automapper"),
    s"git@github.com:bfil/scala-automapper.git"
  ))
)
