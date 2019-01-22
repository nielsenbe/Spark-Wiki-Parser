package com.github.nielsenbe.sparkwikiparser.wikipediasparkrunner

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

class ParserWorkflow {
  def ParseFileAndCreateDatabase(spark: SparkSession, sourceFile: String, destDB: String): Unit = {
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

    dbRunner.CreateBaseAndViews(spark, items, destDB)


    dbRunner.CreateAndPersistDBTables(spark, "wkp_page_simple", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_redirect", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_tag", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_table", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_template", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_template_param", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_text", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_link_external", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_link_wiki", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_header", destDB)
    dbRunner.CreateAndPersistDBTables(spark, "wkp_page", destDB)

    println("Wikipedia Database Creation Complete")
  }

  def printVersion(): Unit = {
    println("Version 1.4")
  }
}
