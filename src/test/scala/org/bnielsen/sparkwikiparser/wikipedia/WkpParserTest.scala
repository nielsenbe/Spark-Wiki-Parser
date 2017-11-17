package test.scala.org.bnielsen.sparkwikiparser.wikipedia

import main.scala.org.bnielsen.sparkwikiparser.wikipedia._
import org.scalatest.FlatSpec

import scala.io.Source

class WkpParserTest extends FlatSpec {

  def FullParseTest(): Unit = {
    val wikiText = InputWikiText(
      Source.fromFile("src\\resources\\Test_Actual.txt").mkString,
      "preserve")


    val revision =  InputRevision(
      "2017-03-26T21:22:45Z",
      "Rescued 1 archive link; remove 1 link. [[User:Green Cardamom/WaybackMedic_2.1|Wayback Medic 2.1]]",
      "text/x-wiki<",
      772364794,
      wikiText)

    val page = InputPage(
      39,
      0,
      null,
      None,
      "Albedo",
      revision)

    val config = WkpParserConfiguration(true, true, true, true, true)

    for(i <- 1 to 100) {
      val t0 = System.nanoTime()
      val article = WkpParser.parseWikiText(page, config)
      println("Total: " + (System.nanoTime() - t0)/1000/1000)
    }


    println("Break")
  }
}
