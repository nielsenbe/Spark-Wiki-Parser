package main.scala.org.bnielsen.sparkwikiparser

import main.scala.org.bnielsen.sparkwikiparser.wikipedia._

import scala.io.Source

object LocalTest {
  def main (arg: Array[String]): Unit = {

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


    val t0 = System.nanoTime()
    val article = WkpParser.parseWikiText(page, null)
    println("Total: " + (System.nanoTime() - t0)/1000/1000)

     println("Break")
  }
}

