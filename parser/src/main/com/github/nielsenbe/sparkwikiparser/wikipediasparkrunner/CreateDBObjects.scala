package com.github.nielsenbe.sparkwikiparser.wikipediasparkrunner

import com.github.nielsenbe.sparkwikiparser.wikipedia.WikipediaPage
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.io.Source

class CreateDBObjects() {
  def CreateBaseAndViews(spark: SparkSession, items: Dataset[WikipediaPage], dbLocation: String) {

    import spark.implicits._
    spark.sql(s"CREATE DATABASE IF NOT EXISTS wkp_db LOCATION '$dbLocation'")
    items.createOrReplaceTempView("items")
    items.flatMap(_.tags).createOrReplaceTempView("tags")
    items.flatMap(_.tables).createOrReplaceTempView("tables")
    items.flatMap(_.texts).createOrReplaceTempView("texts")
    items.flatMap(_.headerSections).createOrReplaceTempView("headers")
    items.flatMap(_.links).createOrReplaceTempView("links")
    items.flatMap(_.templates).createOrReplaceTempView("templates")
  }

  def CreateAndPersistDBTables(spark: SparkSession, viewName: String, dbLocation: String): Unit = {

    println(s"Creating: $viewName")

    // Get view definition from file and then create it
    val viewDefinition = Source.fromFile(s"/resources/$viewName.sql").mkString
    spark.sql(viewDefinition)

    val bucketBy = viewName match {
      case "wkp_page" => "id"
      case "wkp_redirect" => "target_page_id"
      case _ => "parent_page_id"
    }

    //Export table to db
    import spark.implicits._
    spark.table(viewName)
      .repartition(40, $"$bucketBy")
      .write
      .format("parquet")
      .mode("overwrite")
      .bucketBy(1, bucketBy)
      .sortBy(bucketBy)
      .saveAsTable(s"wkp_db.$viewName")
  }
}
