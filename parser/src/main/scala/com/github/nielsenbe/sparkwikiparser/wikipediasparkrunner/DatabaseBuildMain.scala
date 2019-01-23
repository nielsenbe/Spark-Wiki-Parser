package com.github.nielsenbe.sparkwikiparser.wikipediasparkrunner

import org.apache.spark.sql.SparkSession

/**
  * Point of entry for the Scala application.
  */
 object DatabaseBuildMain {
  /**
    * Entry
    * @param args 0: base folder path
    *             1: Wikipedia dump file name and path
    *             3: (Optional) database file format
    */
  def main(args: Array[String]): Unit = {
      val spark = SparkSession.builder.appName("BasicDatabaseBuild").getOrCreate()

      /* Get source file and determine destination db location */
      val sourceFile = args(0)
      val databaseName = args(1)
      val fileType = args.lift(2).getOrElse("parquet")

      val wf = new ParserWorkflow()
      wf.ParseFileAndCreateDatabase(spark, sourceFile, databaseName, fileType)
    }
}
