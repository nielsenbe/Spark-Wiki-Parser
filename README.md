(WARNING APP IS IN EXTREME EARLY ALPHA)

# Spark-Wiki-Parser
The goal of the Spark-Wiki-Parser project is to enable researchers and machine learning experts to quickly extract insights and value from the [MediaWiki](https://wikimediafoundation.org/wiki/Our_projects) and [DBpedia]( http://wiki.dbpedia.org/) data dumps.  The project is split into two parts: the Scala based parser and jump start project code.  The Scala parser is platform neutral, but has been designed to work best with Apache Spark.  It reads the data dumps and extracts them to a common simplified format.  The jump start projects are designed to showcase ways to utilize the parser and Apache Spark in AI and machine learning tasks.

## Getting Started
There are multiple entry points depending on your Spark provider(AWS, Azure, or self hosted) and submit method (Spark Submit or Zeppelin/Jupyter).  See our [wiki](https://github.com/nielsenbe/Spark-Wiki-Parser/wiki) to get started.

## Development Progress
MediaWiki is a large family, but at the moment the framework can parse the following MW sources:
* Wikipedia (In progress)
* DBPedia (Future)
* WikiSource (Future)
* Wiktionary (Future)
* Wikidata (Future)

## Project Goals
* Grant researchers easy access to the data stored in the MediaWiki family.
* Focus on content and semantics over formatting and syntax.
* Take advantage of Spark's built in data import and export functionality.
* Provide jump start projects to demonstrate usage.

## Requirements
* Apache Spark cluster 
  * Version 2.0+ (Parser should work on older versions, but all the notebooks are configured for 2.0+)
  * Recommended 40+ cores and 150+ GB RAM
  * This application will work in local / single node clusters, but may not be performant.
* Apache Zeppelin / Jupyter
  * Only needed if the notebooks are being utilized.

## Data Source
This application reads data from the MediaWiki xml dump files.  These are BZ2 compressed XML files.  Wikipedia is the largest (~ 13 GB compressed).  They also break the files into 50 smaller parts (useful for testing).  More information can be found in our documentation.

## Caveats and known limitations
* The initial parsing must be done with the Scala interpreter.
  * Once the xml dump is converted to the intermediate files then any Spark supported language can be used for analysis. (Scala, Java, Python, or R)
* JAR has not been loaded to Maven central yet.
  * Due to the early alpha nature of this app, we have not uploaded the JAR to any repo.
  * This means that a couple extra steps must be taken to load the JAR to a Spark cluster.
* Parser only supports english wikis
  * Other languages will parse, but columns such as Main Article might not work.
* Parsing < ref > tags can be slow.
  * Sweble does not have logic for pulling out citation templates from a < ref > tag.
  * Reparsing the tag contents will increase overall execution time.
  * If this information(references) isn't needed, then it is recommended that parseRefTags be set to false in WkpParserConfiguration.
  
## Built With
[Apache Spark](https://spark.apache.org/)

[Maven](https://maven.apache.org/)

[Apache Zeppelin](https://zeppelin.apache.org/)

[Jupyter](http://jupyter.org/)

[Sweble WikiText](https://github.com/sweble/sweble-wikitext)