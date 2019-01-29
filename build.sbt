name := "spark-wiki-parser"
organization := "com.github.nielsenbe"
version := "0.10"
description := "Parses a Wiki dump file using Spark."
homepage := Some(url("https://github.com/nielsenbe/Spark-Wiki-Parser"))

scalaVersion := "2.11.12"

mainClass in assembly := Some("com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild.DatabaseBuildMain")
test in assembly := {}

libraryDependencies ++= Seq(
  "org.sweble.wikitext" % "swc-engine" % "3.1.9",
  "org.rogach" %% "scallop" % "3.1.5",
  "org.scalactic" % "scalactic_2.11" % "3.0.4",
  "org.scalatest" % "scalatest_2.11" % "3.0.4",
  "org.apache.spark" % "spark-sql_2.11" % "2.4.0" % "provided")