name := "spark-wiki-parser"
organization := "com.github.nielsenbe"
version := "1.0"
description := "Parses a Wiki dump file using Spark."
homepage := Some(url("https://github.com/nielsenbe/Spark-Wiki-Parser"))
scmInfo := Some(ScmInfo(url("https://github.com/nielsenbe/Spark-Wiki-Parser"),
                            "git@github.com:nielsenbe/Spark-Wiki-Parser.git"))
developers := List(Developer(
  "nielsenbe", 
  "Bradley Nielsen", 
  "brad.e.nielsen@gmail.com", 
  url("https://github.com/nielsenbe")))

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishMavenStyle := true

// Add sonatype repository settings
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

scalaVersion := "2.11.12"

mainClass in assembly := Some("com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild.DatabaseBuildMain")
test in assembly := {}

libraryDependencies ++= Seq(
  "org.sweble.wikitext" % "swc-engine" % "3.1.9",
  "org.rogach" %% "scallop" % "3.1.5",
  "org.scalactic" % "scalactic_2.11" % "3.0.4",
  "org.scalatest" % "scalatest_2.11" % "3.0.4",
  "org.apache.spark" % "spark-sql_2.11" % "2.4.0" % "provided")