/** Copyright 2017
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package scala

import main.scala.com.github.nielsenbe.sparkwikiparser.wikipedia._
import org.scalatest._
import scala.io.Source

class WkpParserTest extends FlatSpec {

  private def getCaseClass(fileURI: String): InputPage = {
    // Get wiki text
    val wikiText = InputWikiText(
      Source.fromFile(fileURI).mkString,
      "preserve")

    val revision =  InputRevision(
      "2017-03-26T21:22:45Z",
      "Rescued 1 archive link; remove 1 link. [[User:Green Cardamom/WaybackMedic_2.1|Wayback Medic 2.1]]",
      "text/x-wiki<",
      772364794,
      wikiText)

    InputPage(
      39,
      0,
      null,
      None,
      "ArticleName",
      revision)
  }

  "A WkpParser" should "produce the required element counts" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Actual.txt")
    val article = WkpParser.parseWiki(page, config)

    assert(article.parserMessage === "SUCCESS")
    assert(article.headerSections.size === 22)
    assert(article.texts.size === 22)
    assert(article.templates.size === 52)
    assert(article.links.size === 173)
    assert(article.tags.size === 10)
    assert(article.tables.size === 5)
  }

  it should "correctly parse headers" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Headers.txt")
    val article = WkpParser.parseWiki(page, config)

    assert(article.parserMessage === "SUCCESS")
    assert(article.headerSections.size === 6)
    assert(article.texts.size === 5)
    assert(article.templates.size === 1)
    assert(article.links.size === 0)
    assert(article.tags.size === 0)
    assert(article.tables.size === 0)

    // Assert lead is correctly formatted
    assert(article.headerSections.head.headerId === 0)
    assert(article.headerSections.head.title === "LEAD")
    assert(article.headerSections.head.level === 0)

    // Assert nested headers are correctly formatted
    assert(article.headerSections(1).headerId === 1)
    assert(article.headerSections(1).title === "HEADER2")
    assert(article.headerSections(1).level === 1)

    // Header with template in it
    assert(article.headerSections(5).title === "HEADER5")
  }


  it should "correctly parse text" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Text.txt")
    val article = WkpParser.parseWiki(page, config)

    assert(article.parserMessage === "SUCCESS")
    assert(article.headerSections.size === 5)
    assert(article.texts.size === 5)
    assert(article.templates.size === 0)
    assert(article.links.size === 2)
    assert(article.tags.size === 0)
    assert(article.tables.size === 2)

    assert(article.texts(0).parentPageId === 39)
    assert(article.texts(0).parentHeaderId === 0)
    assert(article.texts(0).text === "Lead Text italics bold bold italics")

    assert(article.texts(1).parentHeaderId === 1)
    assert(article.texts(1).text === "Header2 text")
    assert(article.texts(2).text === "Header3 text")
    assert(article.texts(3).text === "")
    assert(article.texts(4).text === "")
  }

  it should "correctly parse links" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Links.txt")
    val article = WkpParser.parseWiki(page, config)

    assert(article.parserMessage === "SUCCESS")
    assert(article.headerSections.size === 1)
    assert(article.texts.size === 1)
    assert(article.templates.size === 0)
    assert(article.links.size === 7)
    assert(article.tags.size === 0)
    assert(article.tables.size === 0)

    // Assert lead is correctly formatted
    assert(article.links(0).parentPageId === 39)
    assert(article.links(0).parentHeaderId === 0)
    assert(article.links(0).elementId === 0)
    assert(article.links(0).destination === "WikiLink")
    assert(article.links(0).text === "WikiLink")
    assert(article.links(0).linkType === "WIKIMEDIA")
    assert(article.links(0).subType === "WIKIPEDIA")
    assert(article.links(0).pageBookmark === "")

    // Display text
    assert(article.links(1).elementId === 1)
    assert(article.links(1).text === "TITLE TEXT")

    // Image link
    assert(article.links(2).destination === "File:Test.svg")
    assert(article.links(2).text === "IMAGE TEXT")
    assert(article.links(2).subType === "FILE")

    // Namespace link
    assert(article.links(3).destination === "Category:Climate forcing")
    assert(article.links(3).subType === "CATEGORY")

    // Page bookmark
    assert(article.links(4).pageBookmark === "PAGEBOOKMARK")

    // External link
    assert(article.links(5).destination === "https://nsidc.org/cryosphere/seaice/processes/albedo.html")
    assert(article.links(5).subType === "nsidc.org")
    assert(article.links(5).linkType === "EXTERNAL")

    // Internal bookmark
    assert(article.links(6).destination == "ArticleName")
    assert(article.links(6).pageBookmark == "INTERNALBOOKMARK")
  }

  it should "correctly parse templates" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Templates.txt")
    val article = WkpParser.parseWiki(page, config)

    assert(article.parserMessage === "SUCCESS")
    assert(article.headerSections.size === 1)
    assert(article.texts.size === 1)
    assert(article.templates.size === 11)
    assert(article.links.size === 1)
    assert(article.tags.size === 0)
    assert(article.tables.size === 1)

    // Parameterless template
    assert(article.templates(0).parentPageId === 39)
    assert(article.templates(0).parentHeaderId === 0)
    assert(article.templates(0).elementId === 0)
    assert(article.templates(0).templateType === "No Param Template")
    assert(article.templates(0).parameters.size === 0)

    // Named parameter
    assert(article.templates(1).templateType === "Named Param Template")
    assert(article.templates(1).elementId === 1)
    assert(article.templates(1).parameters.size === 1)
    assert(article.templates(1).parameters(0)._1 === "date")
    assert(article.templates(1).parameters(0)._2 === "JUNE")

    // Positional parameter
    assert(article.templates(2).templateType === "Not Named Param Template")
    assert(article.templates(2).parameters(0)._1 === "*POS_0")
    assert(article.templates(2).parameters(0)._2 === "VALUE1")

    // Nested template
    assert(article.templates(3).templateType === "Nested Template")
    assert(article.templates(4).templateType === "Inner Template")

    // Template with URL
    assert(article.templates(5).parameters(0)._1 === "url")
    assert(article.templates(5).parameters(0)._2 === "http://google.com")
    assert(article.links(0).linkType === "EXTERNAL")
    assert(article.links(0).text === "http://google.com")
    assert(article.links(0).destination === "http://google.com")

    // Escaped characters
    assert(article.templates(6).templateType === "Template with escaped characters")
    assert(article.templates(6).parameters(0)._2 === "TITLETITLE")
    assert(article.templates(7).templateType === "!")

    // Template nested in table
    assert(article.templates(8).templateType === "Template in Cell")

    // Reflist
    assert(article.templates(9).templateType === "Reflist")

    // Template in ref tag
    assert(article.templates(10).templateType === "Cite journal")
  }

  it should "correctly parse tables" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Tables.txt")
    val article = WkpParser.parseWiki(page, config)

    assert(article.parserMessage === "SUCCESS")
    assert(article.headerSections.size === 1)
    assert(article.texts.size === 0)
    assert(article.templates.size === 1)
    assert(article.links.size === 1)
    assert(article.tags.size === 0)
    assert(article.tables.size === 1)

    assert(article.tables.head.parentPageId === 39)
    assert(article.tables.head.elementId === 2)
    assert(article.tables.head.caption === "TABLE HEADER")
    assert(article.tables.head.html === "<table><tr><th>HEADER 1</th><th>HEADER2</th></tr><tr><td>CellContents1</td><td></td></tr><tr><td>CellContents1</td><td>Link in Cell</td></tr></table>")
  }

  it should "correctly parse lists" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Lists.txt")
    val article = WkpParser.parseWiki(page, config)

    assert(article.tables.size === 3)

    assert(article.tables.head.tableHtmlType === "UL")
    assert(article.tables.head.html === "<ul><li>UnOrdered List Item 1UOLWikiLink1</li><li>UnOrdered List Item 2</li><ul><li>UnOrdered List Item 3</li><ul><li>UnOrdered List Item 4</li></ul></ul></ul>")
    assert(article.tables(1).tableHtmlType === "OL")
    assert(article.tables(1).html === "<ol><li>OLWikiLink1Text1</li><li>OLWikiLink2Text2</li><li>Text3</li><ol><li>Text4</li><ol><li>Text5</li></ol></ol></ol>")
    assert(article.tables(2).tableHtmlType === "DL")
    assert(article.tables(2).html === "<dl><dt>Definition lists</dt><dt>item</dt><dd>definition</dd></dl>")

  }

  it should "correctly parse blank pages" in {
    val config = WkpParserConfiguration(true, true, true, true, true, true)
    val page = getCaseClass("parser\\src\\resources\\Test_Blank.txt")
    val article = WkpParser.parseWiki(page, config)
  }
}