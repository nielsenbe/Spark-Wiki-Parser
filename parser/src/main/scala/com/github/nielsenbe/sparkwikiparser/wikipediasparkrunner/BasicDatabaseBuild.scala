import com.github.nielsenbe.sparkwikiparser.wikipediasparkrunner.ParserWorkflow
import org.apache.spark.sql.SparkSession

 object BasicDatabaseBuild {
    def main(args:Array[String]): Unit ={
      val spark = SparkSession.builder.appName("BasicDatabaseBuild").getOrCreate()

      /* Get target wikipedia dump */
      val folder = "MNT/wiki/"
      val sourceFile = "enwiki-20190101-pages-articles-multistream.xml.bz2"
      val destFolder = folder + "wkp_db"

      val source = folder + sourceFile
      val destDB = folder + destFolder + "/"

      val wf = new ParserWorkflow()
      wf.ParseFileAndCreateDatabase(spark, source, destDB)
    }
}
