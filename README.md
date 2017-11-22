(WARNING APP IS IN EXTREME EARLY ALPHA)

# Spark-Wiki-Parser
Spark-Wiki-Parser is an Apache Spark based framework for parsing and extracting MediaWiki dumps (Wikipedia, Wiktionary, Wikidata).  It uses the Sweble parser to do the initial syntax tree generation.  The app then cleans, enriches, and condenses the tree down to a flattened format.  From there it can be exported to JSON, CSV, Parquet, etc.

# Development Progress
MediaWiki is a large family, but at the moment the framework can parse the following MW sources:
* Wikipedia (In progress)
* WikiSource (Future)
* Wiktionary (Future)
* Wikidata (Future)
* DBPedia (Future)

## Documentation
We use the Github [wiki](https://github.com/nielsenbe/Spark-Wiki-Parser/wiki)

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

## Languages
The Sweble parser is Java based and this wrapper is written in Scala.  The initial parsing must be done with the Scala interpreter.  Once the xml dump is converted to the intermediate files then any Spark supported language can be used for analysis. (Scala, Java, Python, or R)

## Data Source
This application reads data from the MediaWiki xml dump files.  These are BZ2 compressed XML files.  Wikipedia is the largest (~ 13 GB compressed).  They also break the files into 50 smaller parts (useful for testing).  More information can be found in our documentation.

## Caveats and known limitations
* JAR has not been loaded to Maven central yet.
  * Due to the early alpha nature of this app, we have not uploaded the JAR to any repo.
  * This means that a couple extra steps must be taken to load the JAR to a Spark cluster.
* Parser only supports english wikis
  * Other languages will parse, but columns such as Main Article might not work.
* Parser only works with current edit
  * Backups with all history are available, but they will not work with current version.
* Parsing < ref > tags can be slow.
  * Sweble does not have logic for pulling out citation templates from a < ref > tag.
  * Reparsing the tag contents will increase overall execution time.
  * If this information(references) isn't needed, then it is recommended that parseRefTags be set to false in WkpParserConfiguration.