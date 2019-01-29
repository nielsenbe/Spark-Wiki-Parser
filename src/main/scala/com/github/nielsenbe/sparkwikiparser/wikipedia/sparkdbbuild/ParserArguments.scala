package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import org.rogach.scallop.{ScallopConf, ScallopOption}

/**
  * For command line arguments
  *
  * @param arguments from main function
  */
class ParserArguments(arguments: Seq[String]) extends ScallopConf(arguments) {
  val dumpFile = opt[String](
    name="dumpfile",
    required=true,
    descr="Full path of dump file.  Path format will vary depending on source system(HDFS, S3, DFS, etc).")
  val destFolder: ScallopOption[String] = opt[String](
    name="destloc",
    required=true,
    descr="Path to put the database, if null then Spark default will be used.")
  val destFormat: ScallopOption[String] = opt[String](
    name="destformat",
    required=false,
    default=Some("parquet"),
    descr="(opt)File format for DB tables.  Default is parquet.")

  verify()
}

case class Arguments(dumpFile: String, destFolder: String, destFormat: String)