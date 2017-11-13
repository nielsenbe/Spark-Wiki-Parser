# Spark-Wiki-Parser
Spark-Wiki-Parser is an Apache Spark based framework for parsing and extracting MediaWiki dumps (Wikipedia, Wiktionary, Wikidata).  It uses the Sweble parser to do the initial syntax tree generation.  The app then cleans, enriches, and condenses the tree down to a flattened format.  From there it can be exported to JSON, CSV, Parquet, etc.

## Project Goals
* Grant researchers easy access to the data stored in the MediaWiki family.
* Focus on content and semantics over formatting and syntax.
* Take advantage of Spark's built in data import and export functionality.
* Provide Apache Zeppelin notebooks (for Azure HDInsight and AWS EMR) to minimize the hassle.

## Requirements
* Apache Spark cluster 
  * Version 2.0+ (Parser will work with older versions, but all the notebooks are configured for 2.0+)
  * Preferably 40+ cores and 150+ GB RAM
  * This application will work in local / single node clusters, but may not be performant.
* Apache Zeppelin
  * Only needed if the notebooks are being utilized.

## Introduction
Spark-Wiki-Parser is a framework for researchers who want to use Apache Spark to analyze MediaWiki data.  MediaWiki is a large family, but at the moment the framework can parse the following MW sources:
* Wikipedia
* Wiktionary (Future)
* Wikidata (Future)

This framework does not contain code for the raw parsing of the Wiki markup.  Rather it acts as a wrapper for the Sweble parser.  Sweble produces a rather deep and complex abstract syntax tree.  Most of the code in this project revolves around taking that syntax tree and flattening, cleaning, and enriching it.  For example an ordinary wiki link:

WtPage

-- WtSection

--- WtBody

---- WtInternalLink

----- WtPageName

------ WtText (Destination)

----- WtLinkTitle

Our framework condenses it down to:

Article

-- Link(destination, text, linkType, subType, pageBookmark)

We call this the simplified syntax tree or simple tree for short.

Once the simple tree has been generated it is easy to save it to the desired format.  The framework's default persistance method is Parquet, but many others will work.

## Source
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
* Parser only works with english
  * Other languages will parse, but columns such as Main Article might not work.
* Parser only works with current edit
  * Backups with all history are available, but they will not work with current version.
* Sweble parser doesn't always pick up items nested in <ref>
  * Right now this can be fixed by reparsing the nested next.  However, this is resource intensive and is turned off by default.

