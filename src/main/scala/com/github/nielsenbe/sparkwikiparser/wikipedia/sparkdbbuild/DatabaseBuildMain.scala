package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import org.apache.spark.sql.SparkSession


 object DatabaseBuildMain {
  /**
    * Entry
    * @param args 0: base folder path
    *             1: Wikipedia dump file name and path
    *             2: (Optional) database file format
    *             3: database location
    */
  def main(args: Array[String]): Unit = {

    val conf = new ParserArguments(args)
    if (args.length == 0 || args(0) == "--help") conf.printHelp()

    val spark = SparkSession.builder.appName("BasicDatabaseBuild").getOrCreate()

    val wf = new DatabaseBuild()
    wf.parseFileAndCreateDatabase(spark, conf)
    }
}
