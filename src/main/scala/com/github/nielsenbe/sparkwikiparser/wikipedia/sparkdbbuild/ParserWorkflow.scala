package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

/**
  * Workflow of steps needed to convert dump file into a hive database
  */
class ParserWorkflow {
  /**
    * Workflow of steps needed to convert dump file into a hive database
    * @param spark      spark session
    * @param sourceFile uri for the dump file.  Format depends on source file system
    * @param destDBName     name of the hive database to store the files in
    * @param destDBFormat   format of the tables (hive files)
    */
  def ParseFileAndCreateDatabase(spark: SparkSession, sourceFile: String, destDBName: String, destDBLocation: String, destDBFormat: String): Unit = {
    println("Starting initial parse of Wikitext")
    /* Run through initial parse */
    val parsedItems = new InitialWikiParse().GetWikipediaAsDataSet(spark, sourceFile)

    /* Persist the results to make the next steps faster */
    val items = parsedItems.persist(StorageLevel.MEMORY_AND_DISK)

    /*-- Print parse status --*/
    val pageCount = items.count
    println("Wikitext parsing complete")
    println(s"$pageCount in total parsed")

    val errors = items.filter(_.parserMessage != "SUCCESS")
    val errorCount = errors.count

    println(s" $errorCount failed")

    /*-- Clean the results and store them in parquet --*/
    println("Starting flatten/clean process")
    val dbRunner = new CreateDBObjects()

    dbRunner.CreateBaseAndViews(spark, items, destDBName, destDBLocation)


    dbRunner.CreateAndPersistDBTables(spark, "wkp_page_simple", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_redirect", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_tag", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_table", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_template", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_template_param", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_text", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_link_external", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_link_wiki", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_header", destDBName, destDBFormat)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_page", destDBName, destDBFormat)

    println("Wikipedia Database Creation Complete")
  }

  def printVersion(): Unit = {
    println("Version 1.4")
  }
}
