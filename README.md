(WARNING APP IS IN EXTREME EARLY ALPHA)

# Spark-Wiki-Parser
Spark-Wiki-Parser is an Apache Spark based framework for parsing and extracting MediaWiki dumps (Wikipedia, Wiktionary, Wikidata).  It uses the Sweble parser to do the initial syntax tree generation.  The app then cleans, enriches, and condenses the tree down to a flattened format.  From there it can be exported to JSON, CSV, Parquet, etc.

## Project Goals
* Grant researchers easy access to the data stored in the MediaWiki family.
* Focus on content and semantics over formatting and syntax.
* Take advantage of Spark's built in data import and export functionality.
* Provide Apache Zeppelin notebooks (for Azure HDInsight and AWS EMR) to minimize hassle.

## Requirements
* Apache Spark cluster 
  * Version 2.0+ (Parser should work on older versions, but all the notebooks are configured for 2.0+)
  * Recommended 40+ cores and 150+ GB RAM
  * This application will work in local / single node clusters, but may not be performant.
* Apache Zeppelin / Jupyter
  * Only needed if the notebooks are being utilized.

## Overview
Spark-Wiki-Parser is a framework for researchers who want to use Apache Spark to analyze MediaWiki data.  MediaWiki is a large family, but at the moment the framework can parse the following MW sources:
* Wikipedia
* WikiSource (Future)
* Wiktionary (Future)
* Wikidata (Future)
* DBPedia (Future)

This framework does not contain code for the raw parsing of the Wiki markup.  Rather it acts as a wrapper for the Sweble parser.  Sweble produces a deep and complex abstract syntax tree.  Most of the code in this project revolves around taking that syntax tree and flattening, cleaning, and enriching it.

The output of the parser is a simplified syntax tree or simple tree for short.
Once the simple tree has been generated it is easy to save it to the desired format.  The framework's default persistence method is Parquet, but many others will work.

## Data Source
This application reads data from the MediaWiki xml dump files.  These are BZ2 compressed XML files.  Wikipedia is the largest (~ 13 GB compressed).  They also break the files into 50 smaller parts (useful for testing).  
Main site:

[Wikimedia Downloads](https://dumps.wikimedia.org/backup-index.html)

[Mirrors](https://dumps.wikimedia.org/mirrors.html)

* enwiki = English Wikipedia
 * enwiki-[date]-pages-articles.xml.bz2 = Full backup
 * enwiki-[date]-pages-articles1.xml-p[id].bz2 = Full backup divided into 50 pieces

All the notebooks use Databrick's XML source to parse the file.  Other methods are available, but this method has been tested and validated.
[Databrick's XML](https://github.com/databricks/spark-xml)

## Caveats and known limitations
* Parser only supports english wikis
  * Other languages will parse, but columns such as Main Article might not work.
* Parser only works with current edit
  * Backups with all history are available, but they will not work with current version.
* Parsing < ref > tags can be slow.
  * Sweble does not have logic for pulling out citation templates from a < ref > tag.
  * Reparsing the tag contents will increase overall execution time.
  * If this information isn't needed, then it is recommended that parseRefTags be set to false in WkpParserConfiguration.

