package com.github.tashoyan.tfidf

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.scalatest.{BeforeAndAfter, FunSuite}

class TfIdfTest extends FunSuite with BeforeAndAfter {

  private val sample: Seq[Seq[String]] = Seq(
    "one flesh one bone one true religion",
    "all flesh is grass",
    "one is all all is one"
  )
    .map(_.split("\\s+").toSeq)

  var spark: SparkSession = _

  before {
    spark = SparkSession.builder()
      .master("local[*]")
      .config("spark.ui.enabled", "false")
      .config("spark.sql.warehouse.dir", "target/spark-warehouse")
      .getOrCreate()
  }

  after {
    spark.stop()
  }

  private def sampleDf = {
    val spark0 = spark
    import spark0.implicits._
    sample.toDF("document")
      .withColumn("another_column", lit(10))
  }

  /*Expected values for TF*IDF are generated by Spark API: CountVectorizer + IDF*/
  test("genTfIdf") {
    val config = TfIdfConfig()
    val tfIdf = new TfIdf(config)
    val result = tfIdf.genTfIdf(sampleDf)
      .cache()
    /*Look at the results*/
    //    result
    //      .select(
    //        config.docIdColumn,
    //        config.documentColumn,
    //        config.tokenColumn,
    //        config.tfColumn,
    //        config.dfColumn,
    //        config.idfColumn,
    //        config.tfIdfColumn,
    //        "another_column"
    //      )
    //      .orderBy(
    //        col(config.docIdColumn),
    //        col(config.tfIdfColumn).desc
    //      )
    //      .show(false)

    val columns = result.columns
    config.productIterator.foreach { mandatoryColumn: Any =>
      assert(columns.contains(mandatoryColumn.asInstanceOf[String]), s"Mandatory column $mandatoryColumn is expected")
    }

    val anotherColumn = result.select("another_column")
      .collect()
      .map(_.getAs[Int](0))
    assert(anotherColumn.forall(_ == 10), "Custom column another_column must present and must have original value")

    /* Doc 0*/

    val doc0Tokens = result
      .select("token", "tf_idf", "document")
      .collect()
      .map(row => (row.getAs[String](0), row.getAs[Double](1), row.getAs[Seq[String]](2)))
      .filter(_._3 == Seq("one", "flesh", "one", "bone", "one", "true", "religion"))
      .map(v => (v._1, v._2))
    assert(doc0Tokens.length === 5, "Number of distinct tokens in doc 0")

    val doc0One = doc0Tokens.filter { case (token, _) => token == "one" }
    assert(doc0One.length === 1, "Number of entries for token 'one' in doc 0")
    assert(doc0One.head._2 == 0.8630462173553426, "TF*IDF for token 'one' in doc 0")

    val doc0Flesh = doc0Tokens.filter { case (token, _) => token == "flesh" }
    assert(doc0Flesh.length === 1, "Number of entries for token 'flesh' in doc 0")
    assert(doc0Flesh.head._2 === 0.28768207245178085, "TF*IDF for token 'flesh' in doc 0")

    /* Doc 1*/

    val doc1Tokens = result
      .select("token", "tf_idf", "document")
      .collect()
      .map(row => (row.getAs[String](0), row.getAs[Double](1), row.getAs[Seq[String]](2)))
      .filter(_._3 == Seq("all", "flesh", "is", "grass"))
      .map(v => (v._1, v._2))
    assert(doc1Tokens.length === 4, "Number of distinct tokens in doc 1")

    val doc1Flesh = doc1Tokens.filter { case (token, _) => token == "flesh" }
    assert(doc1Flesh.length === 1, "Number of entries for token 'flesh' in doc 1")
    assert(doc1Flesh.head._2 == 0.28768207245178085, "TF*IDF for token 'flesh' in doc 1")

    val doc1Grass = doc1Tokens.filter { case (token, _) => token == "grass" }
    assert(doc1Grass.length === 1, "Number of entries for token 'grass' in doc 1")
    assert(doc1Grass.head._2 === 0.6931471805599453, "TF*IDF for token 'grass' in doc 1")

    /* Doc 2*/

    val doc2Tokens = result
      .select("token", "tf_idf", "document")
      .collect()
      .map(row => (row.getAs[String](0), row.getAs[Double](1), row.getAs[Seq[String]](2)))
      .filter(_._3 == Seq("one", "is", "all", "all", "is", "one"))
      .map(v => (v._1, v._2))
    assert(doc2Tokens.length === 3, "Number of distinct tokens in doc 2")

    val doc2One = doc2Tokens.filter { case (token, _) => token == "one" }
    assert(doc2One.length === 1, "Number of entries for token 'one' in doc 2")
    assert(doc2One.head._2 === 0.5753641449035617, "TF*IDF for token 'one' in doc 2")

    val doc2All = doc2Tokens.filter { case (token, _) => token == "all" }
    assert(doc2All.length === 1, "Number of entries for token 'all' in doc 2")
    assert(doc2All.head._2 === 0.5753641449035617, "TF*IDF for token 'all' in doc 2")

  }
}
