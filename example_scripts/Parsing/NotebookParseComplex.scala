/*********************************

This code is designed to be run from a notebook/Spark Shell.

You will need to add the following references to the interpreter.
com.databricks:spark-xml_2.11:0.5.0
com.github.com.nielsenbe.sparkwikiparser:1.0.0

**********************************/

import com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild._
import com.github.nielsenbe.sparkwikiparser.wikipedia.WikipediaPage
import org.apache.spark.sql
import org.apache.spark.sql.SparkSession


/*********************************

Establish the dump file location, output location, and output format.

- Full path of dump file.  Path format will vary depending on source system(HDFS, S3, DFS, etc).
- Path to put folder that will house the export files.
- (opt)File format for export file.  Default is parquet.

**********************************/

val args = Arguments(
    "[Dump file location]",
    "[Output file location",
    "[Format]")
	
/*********************************

Execute the build process.

**********************************/



/* Run through initial parse */
println("Starting initial parse of Wikitext")
val parser = new ParserFunctions()
val parsedItems = parser.getWikipediaAsDataSet(spark, args)

parsedItems.write.mode("overwrite").parquet(args.destFolder + "stg/" + "cached_wkp")
import spark.implicits._
val items = spark.read.parquet(args.destFolder + "stg/" + "cached_wkp").as[WikipediaPage]

/* Print parse status */
println("Wikitext parsing complete")
val pageCount = items.count
println(s"$pageCount in total parsed")

val errors = items.filter(_.parserMessage != "SUCCESS").count
println(s" $errors failed")

/*-- Clean the results and store them in parquet --*/
println("Starting flatten/clean process")

val persist = parser.persistToStaging(spark, _:sql.DataFrame, _:String, args)
persist(items.toDF().select(
	$"id",
	$"title",
	$"redirect",
	$"nameSpace",
	$"revisionId",
	$"revisionDate",
	$"parserMessage"),
  "items")
persist(items.flatMap(_.headerSections).toDF(), "headers")
persist(items.flatMap(_.templates).toDF(), "templates")
persist(items.flatMap(_.tables).toDF(), "tables")
persist(items.flatMap(_.texts).toDF(), "texts")
persist(items.flatMap(_.links).toDF(), "links")
persist(items.flatMap(_.tags).toDF(), "tags")

val createFinal = parser.createFinalFile(spark, _:String, args)
createFinal("wkp_page_simple")
createFinal("wkp_redirect")
createFinal("wkp_tag")
createFinal("wkp_table")
createFinal("wkp_template")
createFinal("wkp_template_param")
createFinal("wkp_text")
createFinal("wkp_link_external")
createFinal("wkp_link_wiki")
createFinal("wkp_header")
createFinal("wkp_page")

println("Wikipedia Database Creation Complete")


/*********************************

Files should now be in your Output file location

**********************************/