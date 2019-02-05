
# Spark-Wiki-Parser
Spark-Wiki-Parser is an Apache Spark based parser that is designed with researchers and data scientists in mind.  This parser is one amongst many: [Wikipedia Parsers](https://www.mediawiki.org/wiki/Alternative_parsers) What sets this parser apart?
* The only Apache Spark native parser. 
* Only parser that outputs data to a flattened user friendly tabular structure.
* Parser is accompanied by tutorials to get you started in Wikipedia research.

This repository doesn't have any code for the raw parsing of the Wikitext.  This app outsources that to the [Sweble WikiText Engine](https://github.com/sweble/sweble-wikitext).  Sweble converts the Wikitext into a deeply nested tree.  The Spark-Wiki-Parser takes this tree and cleans and flattens it.

## Getting Started
There are multiple entry points depending on your Spark provider(AWS, Azure, Databricks, or self hosted) and submit method (Spark Submit or Notebook).  See our [wiki](https://github.com/nielsenbe/Spark-Wiki-Parser/wiki) to get started.

## Project Goals
* Grant researchers easy access to the data stored in Wikipedia.
* Focus on content and semantics over formatting and syntax.
* Take advantage of Spark's built in data import and export functionality.
* Provide jump start projects to demonstrate usage.

## Requirements
* Apache Spark cluster 
  * Version 2.0+ (Parser should work on older versions, but has only been tested in 2.0+)
  * Recommended 40+ cores and 150+ GB RAM
  * This application will work in local / single node clusters, but may not be performant.
* Apache Zeppelin / Jupyter / Databricks
  * Only needed if the notebooks are being utilized.

## Data Source
This application reads data from the MediaWiki xml dump files.  These are BZ2 compressed XML files.  Wikipedia is the largest (~ 15 GB compressed).  They also break the files into 50 smaller parts (useful for testing).  More information can be found in our documentation.

## Caveats and known limitations
* The initial parsing must be done with the Scala interpreter.
  * Once the xml dump is converted to the flattened files then any Spark supported language can be used for analysis. (Scala, Java, Python, R, or SQL)
* Parser only supports English wikis
  * Other languages will parse, but columns such as Main Article might not work.
* Has yet to be loaded into a central repository such as Maven.
* Parsing < ref > tags can be slow.
  * Sweble does not have logic for pulling out citation templates from a < ref > tag.
  * Reparsing the tag contents will increase overall execution time.
  * If this information(references) isn't needed, then it is recommended that parseRefTags be set to false in WkpParserConfiguration.
  
## Built With
[Apache Spark](https://spark.apache.org/)

[SBT](https://www.scala-sbt.org/)

[Sweble WikiText](https://github.com/sweble/sweble-wikitext)