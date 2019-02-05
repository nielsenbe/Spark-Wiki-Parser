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

val wf = new DatabaseBuildFull()
wf.parseFileAndCreateDatabase(spark, args)

/*********************************

Files should now be in your Output file location

**********************************/