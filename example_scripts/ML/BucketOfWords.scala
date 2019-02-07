/*****************************************

%md

## Basic Text Logistic Regression Classification

For this first example we pull in text from two headers: Early life and death.  We then apply a logistic regression model to predict if a text is talking about someone's early life or later life.

This is not meant to be an in depth introduction to natural language processing.  Rather a starting point for utilizing wikipedia data and Spark's ML lib.

Adapted from: [Mastering Spark: Example Text Classification](https://jaceklaskowski.gitbooks.io/mastering-apache-spark/spark-mllib/spark-mllib-pipelines-example-classification.html)

See also:

[Tokenization](https://en.wikipedia.org/wiki/Lexical_analysis#Tokenization)

[Feature hashing](https://en.wikipedia.org/wiki/Feature_hashing)

******************************************/

val destfolder = "[Folder Path]"

spark.read.parquet(destfolder + "wkp_header").createOrReplaceTempView("headers")
spark.read.parquet(destfolder + "wkp_text").createOrReplaceTempView("text")


spark.sql("""
CREATE OR REPLACE TEMPORARY VIEW trainingSet
AS
SELECT
    txt.text,
    CASE WHEN hdr.title = 'Early life' THEN 1.0 ELSE 0.0 END as label
FROM
    headers hdr
JOIN
    text txt
    ON  txt.parent_page_id = hdr.parent_page_id 
    AND txt.parent_header_id = hdr.header_id

WHERE
    hdr.title IN('Early life', 'Death')
AND LENGTH(txt.text) > 75
LIMIT 1000
""")


/*****************************************

Create training set

******************************************/

import org.apache.spark.ml.feature.{HashingTF, IDF, Tokenizer}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.Pipeline

// Create training and set
val labeledDf = spark.table("trainingSet")
val Array(trainDF, testDF) = labeledDf.randomSplit(Array(0.75, 0.25))

// Split into individual words
val tokenizer = new Tokenizer()
  .setInputCol("text")
  .setOutputCol("words")

// Hash words
val hashingTF = new HashingTF()
    .setInputCol(tokenizer.getOutputCol)
    .setOutputCol("features")
    .setNumFeatures(5000)

// Create logistic regression model
val lr = new LogisticRegression().setMaxIter(20).setRegParam(0.01)

val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, lr))

val model = pipeline.fit(spark.table("trainingSet"))

val trainPredictions = model.transform(trainDF)
val testPredictions = model.transform(testDF)

trainPredictions.filter("label != prediction").show()

/*****************************************

Model and evaluate

******************************************/

import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.param.ParamMap

val evaluator = new BinaryClassificationEvaluator().setMetricName("areaUnderROC")
val evaluatorParams = ParamMap(evaluator.metricName -> "areaUnderROC")
val areaTrain = evaluator.evaluate(trainPredictions, evaluatorParams)
val areaTest = evaluator.evaluate(testPredictions, evaluatorParams)