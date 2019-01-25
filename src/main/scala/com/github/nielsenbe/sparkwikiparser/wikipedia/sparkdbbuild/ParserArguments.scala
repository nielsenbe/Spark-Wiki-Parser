package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import org.rogach.scallop.{ScallopConf, ScallopOption}

/**
  * For command line arguments
  *
  * @param arguments from main function
  */
class ParserArguments(arguments: Seq[String]) extends ScallopConf(arguments) {
  val dumpFile: ScallopOption[String] = opt[String](
    name="dumpfile",
    required=true,
    descr="Full path of dump file.  Path format will vary depending on source system(HDFS, S3, DFS, etc).")
  val destDBName: ScallopOption[String]  = opt[String](
    name="destdbname",
    required=true,
    descr="Name of the Spark DB that the tables will be loaded into.")
  val destDBLocation: ScallopOption[String] = opt[String](
    name="destdbloc",
    required=false,
    default = Some(""),
    descr="(opt)Path to put the database, if null then Spark default will be used.")
  val destDBFormat: ScallopOption[String] = opt[String](
    name="destdbformat",
    required=false,
    default=Some("parquet"),
    descr="(opt)File format for DB tables.  Default is parquet.")

  verify()
}