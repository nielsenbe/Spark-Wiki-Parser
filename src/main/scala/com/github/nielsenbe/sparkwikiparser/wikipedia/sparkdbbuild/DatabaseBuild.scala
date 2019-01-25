package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

import java.io.InputStream

import com.github.nielsenbe.sparkwikiparser.wikipedia.{InputPage, WikipediaPage, WkpParser, WkpParserConfiguration}
import org.apache.spark.sql.types.{LongType, StringType, StructType}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.io.Source

/**
  * Workflow of steps needed to convert dump file into a spark database
  */
class DatabaseBuild {
  /**
    * Workflow of steps needed to convert dump file into a hive database
    * @param spark      spark session
    * @param sourceFile uri for the dump file.  Format depends on source file system
    * @param destDBName     name of the hive database to store the files in
    * @param destDBFormat   format of the tables (hive files)
    */
  def parseFileAndCreateDatabase(spark: SparkSession, args: ParserArguments ): Unit = {
    println("Starting initial parse of Wikitext")
    /* Run through initial parse */
    val parsedItems = getWikipediaAsDataSet(spark, args)

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
    createBaseAndViews(spark, items, args)


    createAndPersistDBTables(spark, "wkp_page_simple", args)
    createAndPersistDBTables(spark, "wkp_redirect", args)
    createAndPersistDBTables(spark, "wkp_tag", args)
    createAndPersistDBTables(spark, "wkp_table", args)
    createAndPersistDBTables(spark, "wkp_template", args)
    createAndPersistDBTables(spark, "wkp_template_param", args)
    createAndPersistDBTables(spark, "wkp_text", args)
    createAndPersistDBTables(spark, "wkp_link_external", args)
    createAndPersistDBTables(spark, "wkp_link_wiki", args)
    createAndPersistDBTables(spark, "wkp_header", args)
    createAndPersistDBTables(spark, "wkp_page", args)

    println("Wikipedia Database Creation Complete")
  }



  /**
    * Sets up the code for the initial xml file parse
    * @param spark  spark context
    * @param source url to bz2 file.  Exact format depends on source file system.
    * @return Spark Dataset of WikipediaPage
    */
  def getWikipediaAsDataSet(spark: SparkSession, args: ParserArguments): Dataset[WikipediaPage] = {

    /*-- Create Schema --*/
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
      .load(args.dumpFile())
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
      for (page <- part) yield WkpParser.parseWiki(page, config)
    })
  }



    /**
      * Flattens the parsed dataset to non materialized views
      * @param spark spark session
      * @param items Dataset of parsed Wikipedia pages
      * @param destDBName Name of the database to create
      */
    def createBaseAndViews(spark: SparkSession, items: Dataset[WikipediaPage], args: ParserArguments) {
      import spark.implicits._
      println(s"Creating database (if not exists) ${args.destDBName()}")
      spark.sql(s"CREATE DATABASE IF NOT EXISTS ${args.destDBName()}" + (if(args.destDBLocation() == "") "" else s" LOCATION '${args.destDBLocation()}'"))


      items.createOrReplaceTempView("items")
      items.flatMap(_.tags).createOrReplaceTempView("tags")
      items.flatMap(_.tables).createOrReplaceTempView("tables")
      items.flatMap(_.texts).createOrReplaceTempView("texts")
      items.flatMap(_.headerSections).createOrReplaceTempView("headers")
      items.flatMap(_.links).createOrReplaceTempView("links")
      items.flatMap(_.templates).createOrReplaceTempView("templates")
    }



    /**
      * Instantiates the transformation view then save it in desired format
      * @param spark
      * @param viewName
      * @param destDBName
      */
    def createAndPersistDBTables(spark: SparkSession, viewName: String, args: ParserArguments): Unit = {

      println(s"Creating table: $viewName")

      // Get view definition from file and then create it
      val stream : InputStream = getClass.getResourceAsStream(s"/$viewName.sql")
      val viewDefinition= Source.fromInputStream(stream).mkString
      spark.sql(viewDefinition)

      val bucketBy = viewName match {
        case "wkp_page" => "id"
        case "wkp_page_simple" => "id"
        case "wkp_redirect" => "target_page_id"
        case _ => "parent_page_id"
      }

      //Export table to db
      import spark.implicits._
      spark.table(viewName)
        .repartition(40, $"$bucketBy")
        .write
        .format(args.destDBFormat())
        .mode("overwrite")
        .bucketBy(1, bucketBy)
        .sortBy(bucketBy)
        .saveAsTable(s"${args.destDBName()}.$viewName")
    }
  }
