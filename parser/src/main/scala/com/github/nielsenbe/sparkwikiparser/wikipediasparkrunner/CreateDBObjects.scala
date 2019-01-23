package com.github.nielsenbe.sparkwikiparser.wikipediasparkrunner

import java.io.InputStream

import com.github.nielsenbe.sparkwikiparser.wikipedia.WikipediaPage
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.io.Source

/**
  * First flattens the dataset to SQL views.
  * Then uses Spark SQL to generate the database tables
  */
class CreateDBObjects() {
  /**
    * Flattens the parsed dataset to non materialized views
    * @param spark spark session
    * @param items Dataset of parsed Wikipedia pages
    * @param dbName Name of the database to create
    */
  def CreateBaseAndViews(spark: SparkSession, items: Dataset[WikipediaPage], dbName: String) {
    import spark.implicits._
    spark.sql(s"CREATE DATABASE IF NOT EXISTS wkp_db LOCATION '$dbName'")
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
    * @param dbLocation
    */
  def CreateAndPersistDBTables(spark: SparkSession, viewName: String, dbLocation: String, dbFormat: String): Unit = {

    println(s"Creating table: $viewName")

    // Get view definition from file and then create it
    val stream : InputStream = getClass.getResourceAsStream(s"/resources/$viewName.sql")
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
      .format(dbFormat)
      .mode("overwrite")
      .bucketBy(1, bucketBy)
      .sortBy(bucketBy)
      .saveAsTable(s"$dbLocation.$viewName")
  }
}
