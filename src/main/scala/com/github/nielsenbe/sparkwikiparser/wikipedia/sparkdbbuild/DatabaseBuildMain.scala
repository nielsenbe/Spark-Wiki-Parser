package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import org.apache.spark.sql.SparkSession
import org.rogach.scallop._

/**
  * Point of entry for the Scala application.
  */
 object DatabaseBuildMain {
  /**
    * Entry
    * @param args 0: base folder path
    *             1: Wikipedia dump file name and path
    *             2: (Optional) database file format
    *             3: database location
    */
  def main(args: Array[String]): Unit = {

    if(args.length == 0) println("""
Please supply parameters:
--sourceFile        Full path of dump file.  Path format will vary depending on source system(HDFS, S3, DFS, etc).
--destDBName        Name of the Spark DB that the tables will be loaded into.
--destDBLocation    (Optional) Path to put the database, if null then Spark default will be used.
--destDBFormat      (Optional) File format for DB tables.  Default is parquet.
      """")

      /* Get source file and determine destination db location */
      val conf = new Conf(args)  // Note: This line also works for "object Main extends App"

      val spark = SparkSession.builder.appName("BasicDatabaseBuild").getOrCreate()

      val wf = new ParserWorkflow()
      wf.ParseFileAndCreateDatabase(spark, conf.sourceFile(), conf.destDBName(), conf.destDBLocation(), conf.destDBFormat())
    }
}

/**
  * For command line arguments
  * @param arguments from main function
  */
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  val sourceFile: ScallopOption[String] = opt[String](required=true)
  val destDBName: ScallopOption[String]  = opt[String](required=true)
  val destDBLocation: ScallopOption[String] = opt[String](default = Some(""))
  val destDBFormat: ScallopOption[String] = opt[String](default=Some("parquet"))

  verify()
}
