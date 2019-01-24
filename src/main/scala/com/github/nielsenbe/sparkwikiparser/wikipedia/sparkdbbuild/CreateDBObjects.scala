package com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild

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
    * @param destDBName Name of the database to create
    */
  def CreateBaseAndViews(spark: SparkSession, items: Dataset[WikipediaPage], destDBName: String, destDBLocation: String) {
    import spark.implicits._
    println(s"Creating database (if not exists) $destDBName")
    spark.sql(s"CREATE DATABASE IF NOT EXISTS $destDBName" + (if(destDBLocation == "") "" else s" LOCATION '$destDBName'"))


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
  def CreateAndPersistDBTables(spark: SparkSession, viewName: String, destDBName: String, destDBFormat: String): Unit = {

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
      .format(destDBFormat)
      .mode("overwrite")
      .bucketBy(1, bucketBy)
      .sortBy(bucketBy)
      .saveAsTable(s"$destDBName.$viewName")
  }
}
