package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import java.io.InputStream

import com.github.nielsenbe.sparkwikiparser.wikipedia.{InputPage, WikipediaPage, WkpParser, WkpParserConfiguration}
import org.apache.spark.sql
import org.apache.spark.sql.types.{LongType, StringType, StructType}
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.io.Source

class ParserFunctions {
  /**
    * Sets up the code for the initial xml file parse
    * @param spark  spark context
    * @param args Arguments supplied to program
    * @return Spark Dataset of WikipediaPage
    */
  def getWikipediaAsDataSet(spark: SparkSession, args: Arguments): Dataset[WikipediaPage] = {

    /* Create Schema */
    val schema = new StructType()
      .add("id", LongType, true)
      .add("ns", LongType, true)
      .add("restrictions", StringType, true)
      .add("title", StringType, true)
      .add("redirect", new StructType()
        .add("_VALUE", StringType, true)
        .add("_title", StringType, true),
        true)
      .add("revision", new StructType()
        .add("comment", StringType, true)
        .add("format", StringType, true)
        .add("id", LongType, true)
        .add("minor", StringType, true)
        .add("model", StringType, true)
        .add("parentid", LongType, true)
        .add("sha1", StringType, true)
        .add("timestamp", StringType, true)
        .add("contributor", new StructType()
          .add("id", LongType, true)
          .add("ip", StringType, true)
          .add("username", StringType, true),
          true)
        .add("text", new StructType()
          .add("_VALUE", StringType, true)
          .add("_space", StringType, true),
          true),
        true)

    /* Read into dataset */
    import spark.implicits._
    val ds = spark.read
      .format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .schema(schema)
      .load(args.dumpFile)
      .as[InputPage]

    /*-- Prepare the Wikitext parser --*/
    val config = WkpParserConfiguration(
      parseText = true,
      parseTemplates = true,
      parseLinks = true,
      parseTags = true,
      parseTables = true,
      parseRefTags = false)

    ds.mapPartitions(part => {
      for (page <- part) yield WkpParser.parseWiki(page, config)
    })
  }


  /**
    * Flattens the parsed dataset to non materialized views
    * @param spark spark session
    * @param viewName name for temporary view
    * @param args Arguments supplied to program
    */
  def persistToStaging(spark: SparkSession, items: sql.DataFrame, viewName: String, args: Arguments) {
    items.write.mode("overwrite").parquet(args.destFolder + "stg/flat/" + viewName)
    val staged = spark.read.parquet(args.destFolder + "stg/flat/" + viewName)
    staged.createOrReplaceTempView(viewName)
  }


  /**
    * Instantiates the transformation view then save it in desired format
    * @param spark spark session
    * @param fileName name of final file
    * @param args Arguments supplied to program
    */
  def createFinalFile(spark: SparkSession, fileName: String, args: Arguments): Unit = {
    println(s"Creating table: $fileName")

    // Get view definition from file and then create it
    val stream : InputStream = getClass.getResourceAsStream(s"/$fileName.sql")
    val sql= Source.fromInputStream(stream).mkString
    spark.sql(sql)

    spark.table(fileName)
      .write
      .format(args.destFormat)
      .mode("overwrite")
      .save(args.destFolder + fileName)
  }
}
