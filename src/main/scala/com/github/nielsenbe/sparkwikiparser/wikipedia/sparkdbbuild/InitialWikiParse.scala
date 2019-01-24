package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import com.github.nielsenbe.sparkwikiparser.wikipedia.{InputPage, WikipediaPage, WkpParser, WkpParserConfiguration}
import org.apache.spark.sql.types.{LongType, StringType, StructType}
import org.apache.spark.sql.{Dataset, SparkSession}

/**
  * Takes the compressed dump file and turns into into a parsed data set object
  */
class InitialWikiParse {
  /**
    * Sets up the code for the initial xml file parse
    * @param spark  spark context
    * @param source url to bz2 file.  Exact format depends on source file system.
    * @return Spark Dataset of WikipediaPage
    */
  def GetWikipediaAsDataSet(spark: SparkSession, source: String): Dataset[WikipediaPage] = {

    /*-- Create Schema --*/
    val schema = new StructType()
      .add("id",LongType,true)
      .add("ns",LongType,true)
      .add("restrictions",StringType,true)
      .add("title",StringType,true)
      .add("redirect", new StructType()
        .add("_VALUE",StringType,true)
        .add("_title",StringType,true),
        true)
      .add("revision", new StructType()
        .add("comment",StringType,true)
        .add("format",StringType,true)
        .add("id",LongType,true)
        .add("minor",StringType,true)
        .add("model",StringType,true)
        .add("parentid",LongType,true)
        .add("sha1",StringType,true)
        .add("timestamp",StringType,true)
        .add("contributor", new StructType()
          .add("id",LongType,true)
          .add("ip",StringType,true)
          .add("username",StringType, true),
          true)
        .add("text", new StructType()
          .add("_VALUE",StringType,true)
          .add("_space",StringType,true),
          true),
        true)

    /* Read into dataset */
    import spark.implicits._
    val ds = spark.read
      .format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .schema(schema)
      .load(source)
      .limit(1000)
      .as[InputPage]

    /*-- Parse the file --*/
    val config = WkpParserConfiguration(
      parseText = true,
      parseTemplates = true,
      parseLinks = true,
      parseTags = true,
      parseTables = true,
      parseRefTags = false)

    ds.mapPartitions(part => {
      for(page <- part) yield WkpParser.parseWiki(page, config)
    })
  }
}
