package main.scala.org.bnielsen.sparkwikiparser.sparkjobs

import main.scala.org.bnielsen.sparkwikiparser.wikipedia.InputPage
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SQLContext
import com.databricks.spark.xml._
import com.databricks.spark.xml

object ReadWrite {

  def main(args: Array[String]) {

    val spark = SparkSession
      .builder()
      .appName("ReadWrite")
      .getOrCreate()

    val df = spark.read
      .format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .load("wasb://bnielsenhdi2-container@dsvmdisks937.blob.core.windows.net/user/Wikidata/enwiki-20170401-pages-articles2.xml-p30304p88444.bz2")

    import spark.implicits._
    val ds = df.as[InputPage]

    ds.count()

    println("IM HERE")
  }
}
