# Spark-Wiki-Parser
Spark-Wiki-Parser is an Apache Spark based parser that is designed with researchers and data scientists in mind.  This parser is one amongst many: [Wikipedia Parsers](https://www.mediawiki.org/wiki/Alternative_parsers) What sets this parser apart?
* The only Apache Spark native parser. 
* Only parser that outputs data to a flattened user friendly tabular structure.

This repository doesn't have any code for the raw parsing of the Wikitext.  This app outsources that to the [Sweble WikiText Engine](https://github.com/sweble/sweble-wikitext).  Sweble converts the Wikitext into a deeply nested tree.  The Spark-Wiki-Parser takes this tree and cleans and flattens it.

## Output files
Parser will output files in format of your choice.  Default is Parquet.
* Redirect
* Page
* Header
* External Link
* Wiki Link
* Table
* Tag
* Template
* Template Parameters
* Text

See our [data dictionary](https://github.com/nielsenbe/Spark-Wiki-Parser/wiki/Parser-Wikipedia-Data-Dictionary) for more details.

## Quick Start
Download the [Wikipedia dump file](https://dumps.wikimedia.org/) and upload it to your Spark cluster

Add the following Maven dependencies to your notebook environment.
```
com.databricks:spark-xml_2.11:0.5.0
com.github.nielsenbe:spark-wiki-parser_2.11:1.0
```
Execute the following code.
```scala
import com.github.nielsenbe.sparkwikiparser.wikipedia.sparkdbbuild._

// Spark accessible (HDFS, S3, ADLS, DBFS, etc) location of the dump file in bz2 format.
val dumpFile = "[dump file location]"
// Folder to hold the final parsed files.
val destinationFolder = "[dest folder]"
// Parquet, ORC, JSON, or CSV
val destinationFormat = "parquet"
// Instead of holding the intermediate Dataset in memory (Dataset.cache()) it will use parquet files as cache.
val lowMemoryMode = false

val args = Arguments(dumpFile, destinationFolder, destinationFormat, lowMemoryMode)

val wf = new DatabaseBuildFull()
// Decode xml file, parse it, flatten it, and clean it.
wf.parseFileAndCreateDatabase(spark, args)
```
See our [wiki](https://github.com/nielsenbe/Spark-Wiki-Parser/wiki) for more details.

## Project Goals
* Grant researchers easy access to the data stored in Wikipedia.
* Focus on content and semantics over formatting and syntax.
* Take advantage of Spark's built in data import and export functionality.
* Provide jump start projects to demonstrate usage.

## Requirements
* Apache Spark cluster 
  * Version 2.0+ (Parser should work on older versions, but has only been tested in 2.0+)
  * Recommended 40+ cores and 150+ GB RAM (For initial parse.)
  * This application will work in local / single node clusters, but may take 8+ hours to parse.
* Apache Zeppelin / Jupyter / Databricks
  * Only needed if the notebooks are being utilized.

## Data Source
This application reads data from the MediaWiki xml dump files.  These are BZ2 compressed XML files.  Wikipedia is the largest (~ 15 GB compressed).  They also break the files into 50 smaller parts (useful for testing).  More information can be found in our documentation.

## Caveats and known limitations
* The initial parsing must be done with the Scala interpreter.
  * Once the xml dump is converted to the flattened files then any Spark supported language can be used for analysis. (Scala, Java, Python, R, or SQL)
* Parser only supports English wikis
  * Other languages will parse, but columns such as Main Article might not work.
* Parsing < ref > tags can be slow.
  * Sweble does not have logic for pulling out citation templates from a < ref > tag.
  * Reparsing the tag contents will increase overall execution time.
  * If this information(references) isn't needed, then it is recommended that parseRefTags be set to false in WkpParserConfiguration.
  
## Built With
[Apache Spark](https://spark.apache.org/)

[SBT](https://www.scala-sbt.org/)

[Sweble WikiText](https://github.com/sweble/sweble-wikitext)