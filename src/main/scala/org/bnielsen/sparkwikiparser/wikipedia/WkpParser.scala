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
package main.scala.org.bnielsen.sparkwikiparser.wikipedia

/** Converts Wikitext to a simplified tree structure
  *
  * Wikipedia articles are coded using a special MediaWiki syntax.  Parsing this text is not simple task and requires a
  * specialized parser.  In our case we use the Sweble wiki parser.  Sweble produces a deep and exact abstract syntax
  * tree of the wiki page.  For most purposes this is overkill.  This class takes the AST and transforms it into a cleaned
  * and simplified version.  The simplified tree classes can be found at:
  * main.scala.org.bnielsen.sparkwikiparser.DomainObjects.wikipediaArticleClasses
  *
  * Once the simplified tree has been generated it can easily be converted to other formats.  Generally it is expected
  * that Apache Spark will handle the IO, but there is nothing stopping it from being used locally.
  */

import java.net.URI
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.engine.{PageId, PageTitle, WtEngineImpl}
import org.sweble.wikitext.parser.nodes.WtNodeList.WtNodeListImpl
import org.sweble.wikitext.parser.nodes._

import scala.collection.JavaConversions._
import scala.util.Try


object WkpParser {

  /** Converts xml class to a simple abstract tree object
    *
    * @param input case class representation of xml element
    * @return WikipediaArticle
    */
  def parseWikiText(input: InputPage, wkpconfig: WkpParserConfiguration): WikipediaArticle = {


    /* Parse top level elements */
    val title: String = Option(input.title).getOrElse("EMPTY")
    val redirectValue: String = Option(input.redirect).map(x => x._title).getOrElse("")
    val redirect: String = Option(redirectValue).getOrElse("")

    val revision = Option(input.revision)
    val revId: Long = revision.map(_.id).getOrElse(0)

    val revTS: String = revision.map(_.timestamp).getOrElse("2000-01-01T00:00:45Z")
    val lastRevision: Long = ZonedDateTime.parse(revTS, DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))).toEpochSecond

    val revText: Option[InputWikiText] = revision.map(_.text)
    val wikiText: String = revText.map(_._VALUE).getOrElse("")

    val qualifiedNameSpace = getQualifiedNamespace(input.ns)

    /* Parse page */
    def parseWikiText(wikiText: String): List[WikipediaElement] = {
      val config = DefaultConfigEnWp.generate
      val engine = new WtEngineImpl(config)
      val pageTitle = PageTitle.make(config, input.title)
      val pageId = new PageId(pageTitle, input.id)
      val cp = engine.postprocess(pageId, wikiText, null)
      val parsedPage = cp.getPage

      val state = new WkpParserState(input.id.intValue(), wkpconfig)
      parseNode(state, parsedPage)
    }

    // Only parse if it is not a redirect
    val elements = redirect match {
      case "" => parseWikiText(wikiText)
      case _ => List.empty
    }

    /* Sort elements */
    val headers = List(WikipediaHeader(input.id.intValue, 0,  "LEAD", 0, None, false)) ::: elements.collect{case n: WikipediaHeader => n}
    val templates = elements.collect{case n: WikipediaTemplate => n}
    val links = elements.collect{case n: WikipediaLink => n}
    val tags = elements.collect{case n: WikipediaTag => n}
    val tables = elements.collect{case n: WikipediaTable => n}

    /* Clean and prepare text */
    def isDuplicateReturn(a: WikipediaText, b: WikipediaText): Boolean = !(a.text == "\n" && b.text == "\n")
    val removedUnnecessaryReturns = elements.collect{case n: WikipediaText => n}.sliding(2).collect{case Seq(a,b) if isDuplicateReturn(a,b) => b}.toList
    val text = removedUnnecessaryReturns
      .groupBy(_.parentHeaderId)
      .mapValues(x => x.map(_.text).mkString(""))
      .map(x => WikipediaText(input.id.intValue(), x._1, x._2)).toList

    /* Classify page */
    val isDisambiguation = templates.exists(x => Set("DISAMBIGUATION", "DISAMBIG", "DISAMBIG-ACRONYM", "DISAMBIGUATION CATEGORY", "DAB").contains(x.templateType))
    val pageType = getPageType(title, redirect, qualifiedNameSpace, isDisambiguation)

    /* Return case class */
    WikipediaArticle(
      input.id,
      input.title,
      redirect,
      qualifiedNameSpace,
      pageType,
      revId,
      lastRevision,
      headers,
      text,
      templates,
      links,
      tags,
      tables)
  }

  /** Classify the wikipedia article.
    * These are logical groupings and are mostly used for filtering articles.
    *
    * @param title wikipedia article title
    * @param redirect redirect string
    * @param nameSpace namespace
    * @param isDisambiguation does the article contain any disambiguation templates
    * @return
    */
  private def getPageType(title: String, redirect: String, nameSpace: String, isDisambiguation: Boolean): String = {

    val titleUpper = title.toUpperCase

    if(!redirect.isEmpty)
      "REDIRECT"
    else if(nameSpace == "Article")
      nameSpace
    else if(titleUpper.contains("DISAMBIGUATION"))
      "DISAMBIGUATION"
    else if(isDisambiguation)
      "DISAMBIGUATION"
    else if(titleUpper.contains("CATEGORY"))
      "CATEGORY"
    else if(titleUpper.startsWith("LIST OF"))
      "LIST"
    else
      "ARTICLE"
  }

  /** Convert name space to text representation.
    *
    * @param ns integer from xml file
    * @return textual name of namespace
    */
  private def getQualifiedNamespace(ns: Long): String = ns match {
    case 0 => "Article"
    case 1 => "Talk"
    case 2  => "User"
    case 4 => "Wikipedia"
    case 6 => "File"
    case 8 => "MediaWiki"
    case 10 => "Template"
    case 12 => "Help"
    case 14 => "Category"
    case 100 => "Portal"
    case 108 => "Book"
    case 118 => "Draft"
    case 446 => "Education"
    case 710 => "TimedText"
    case 828 => "Module"
    case 2300 => "Gadge"
    case 2302 => "Gadget definition"
    case -1 => "Special"
    case -2 => "Media"
    case _ => "Article"
  }

  /** If Sweble doesn't correctly parse a section then fix it and reparse it.
    *
    * @param wikiText text that needs to be reparsed
    * @return List of our simplified nodes
    */
  private def parseWikiTextFragment(state: WkpParserState, wikiText: String): List[WikipediaElement] = {

    val config = DefaultConfigEnWp.generate
    val engine = new WtEngineImpl(config)
    val pageTitle = PageTitle.make(config, "FRAGMENT")
    val pageId = new PageId(pageTitle, 1)
    val cp = engine.postprocess(pageId, wikiText, null)
    val parsedFragment = cp.getPage

    parseNode(state, parsedFragment)
  }

  /** Determine how to parse each node type.
    *
    * @param nodeList Sweble node super type WtNode implements the java list interface.  We cast to this
    *                 for mapping purposes.
    * @return List of our simplified nodes
    */
  private def parseNode(state: WkpParserState, nodeList: java.util.List[WtNode]): List[WikipediaElement] = nodeList.toList flatMap {
      /* Leaf classes */
      case _: WtHeading => List.empty // Don't parse section headers
      case n: WtSection => processHeader(state, n)
      case n: WtText if state.config.parseText => processText(state, n)
      case n: WtXmlEntityRef  if state.config.parseText => processHTMLEntity(state, n)
      case n: WtInternalLink if state.config.parseLinks => processWikiLink(state, n)
      case n: WtImageLink if state.config.parseLinks  => processImageLink(state, n)
      case n: WtExternalLink if state.config.parseLinks  => processExternalLink(state, n)
      case n: WtTemplate if state.config.parseTemplates => processTemplate(state, n)
      case n: WtTagExtension if state.config.parseTags => processTagExtension(state, n)
      case n: WtTable if state.config.parseTables => processTable(state, n)
      /* Container classes */
      case n: WtNodeListImpl => parseNode(state, n)
      case n: WtTableRow => parseNode(state, n)
      case n: WtTableHeader => parseNode(state, n)
      case n: WtTableCell => parseNode(state, n)
      case n: WtTableImplicitTableBody => parseNode(state, n)
      case n: WtTemplateArgument => parseNode(state, n)
      case n: WtXmlElement => parseNode(state, n)
      case n: WtContentNode => parseNode(state, n)
      case _ => List.empty
    }

  /** Header title (==Title1== = H2)
    *
    * @param state current Ids and configuration
    * @param node Sweble node
    * @return
    */
  private def processHeader(state: WkpParserState, node: WtSection): List[WikipediaElement] = {

    // Header Name
    val n = node.asInstanceOf[java.util.List[WtNode]]
    val headerName = getTextFromNode(n(0))

    // Level
    val level = node.getLevel - 1

    // Id
    state.headerId = state.headerIdItr.next()
    val headerId = state.headerId

    // Get nested nodes
    val nodes = parseNode(state, node)

    // Check if a heading has a Main Article template
    def isMainArticle(node: WikipediaTemplate) = Set("MAIN", "MAIN ARTICLE").contains(node.templateType.toUpperCase())
    val mainArticle = nodes.take(3).collect {
      case n: WikipediaTemplate if isMainArticle(n) => if (n.parameters.nonEmpty) n.parameters.head._2 else ""
    } lift 0

    // Determine if it is an ancillary section
    val ancillaryHeaders = Set("REFERENCES", "EXTERNAL LINKS", "SEE ALSO", "NOTES", "BIBLIOGRAPHY", "FURTHER READING", "SOURCES", "FOOTNOTES", "PUBLICATIONS")
    val isAncillary = ancillaryHeaders.contains(headerName.toUpperCase())

    List(WikipediaHeader(state.articleId, headerId , headerName, level, mainArticle, isAncillary)) ::: nodes
  }

  /** Natural language text of an article.  What exactly constitutes this is determined by the parser.
    *
    * @param state current Ids and configuration
    * @param node Sweble Text node
    * @return Single text element (converted to a list for the recursive function)
    */
  private def processText(state: WkpParserState, node: WtText): List[WikipediaElement] = {
    // Standardize line returns
    val content = node.getContent match {
      case "\r\n\r\n" => "\n"
      case "\r" => "\n"
      case "\r\n" => "\n"
      case n => n
    }

    List(WikipediaText(state.articleId, state.headerId, content ))
  }

  /** Internal wikimedia links
    *
    * @param state current Ids and configuration
    * @param node Sweble WtInternalLink  node
    * @return Link and its textual representation.  Links can and often are used as part of the text.
    */
  private def processWikiLink(state: WkpParserState, node: WtInternalLink): List[WikipediaElement] = {

    // Check if link is using bookmark [Page#SectionHeading]
    val hashSplit = node.getTarget.getAsString.split('#')
    val destination = hashSplit lift 0 getOrElse ""
    val pageBookmark = hashSplit lift 1 getOrElse ""

    // If title is empty, use destination
    val text = if(node.hasTitle) getTextFromNode(node.getTitle) else destination

    // Determine the link's namespace [Namespace:Page]
    val nameSpaces = Set("USER", "WIKIPEDIA", "FILE", "MEDIAWIKI", "TEMPLATE", "HELP","CATEGORY", "PORTAL", "BOOK")
    val leftSide = destination.split(':') lift 0 getOrElse ""
    val subType  = nameSpaces find(_ == leftSide.toUpperCase()) getOrElse "WIKIPEDIA"

    List(WikipediaLink(state.articleId, state.headerId, state.elementIdItr.next, destination, text, "WIKIMEDIA", subType, pageBookmark)) :::
      List(WikipediaText(state.articleId, state.headerId, text))
  }

  /** Internal wikimedia image.
    *
    * They are in a slightly different format and require special processing.
    *
    * @param state current Ids and configuration
    * @param node Sweble WtImageLink node
    * @return Link and its textual representation.  Links can and often are used as part of the text.
    */
  private def processImageLink(state: WkpParserState, node: WtImageLink): List[WikipediaElement] = {
    val destination = node.getTarget.getAsString

    val title = if(node.hasTitle) getTextFromNode(node.getTitle) else ""
    val text = if (title == "") destination else title.split('|').last

    List(WikipediaLink(state.articleId, state.headerId, state.elementIdItr.next, destination, text, "WIKIMEDIA", "FILE", "")) :::
      List(WikipediaText(state.articleId, state.headerId, text))
  }

  /** External link
    *
    * @param state current Ids and configuration
    * @param node Sweble WtExternalLink node
    * @return Wikipedia link
    */
  private def processExternalLink(state: WkpParserState, node: WtExternalLink): List[WikipediaElement] = {
    val title = getTextFromNode(node.getTitle)
    val destination = node(0) match {
      case n: WtUrl => n.getProtocol +":" + n.getPath
      case _ => ""
    }

    processExternalLink(state, title, destination)
  }

  // Polymorphic implementation
  private def processExternalLink(state: WkpParserState, title: String, destination: String): List[WikipediaElement] = {
    // check for bookmark
    val hashSplit = destination.split('#')
    val cleanDest = hashSplit(0)
    val pageBookmark = hashSplit lift 1 getOrElse ""

    // For external links, the subtype is its domain.
    def parseURI(uri: String): Try[String] = Try(new URI(uri).getHost)
    val domain = parseURI(cleanDest).getOrElse("INVALID URI")

    List(WikipediaLink(state.articleId, state.headerId, state.elementIdItr.next, cleanDest, title, "EXTERNAL", domain, pageBookmark))
  }

  /** HTML entities: > < & " ' etc
    * Convert them to text
    *
    * @param state current Ids and configuration
    * @param node Sweble WtXmlEntityRef node
    * @return text representation
    */
  private def processHTMLEntity(state: WkpParserState, node: WtXmlEntityRef): List[WikipediaElement] = {
    List(WikipediaText(state.articleId, state.headerId, node.getResolved))
  }

  /** HTML elements such as < math > < / math > (spaces added due to docstring)
    *
    * @param state current Ids and configuration
    * @param node Sweble WtTagExtension node
    * @return Tag element plus any nested elements
    */
  private def processTagExtension(state: WkpParserState, node: WtTagExtension): List[WikipediaElement] = {
    val tagName = node.getName
    val tagBody = node.getBody.getContent

    /*if(tagName.toUpperCase() == "REF")
      parseWikiTextFragment(state.parentArticleId, parentHeaderId, tagBody)
    else*/
      List(WikipediaTag(state.articleId, state.headerId, state.elementIdItr.next, tagName, tagBody))
  }

  /** Templates are a special MediaWiki construct that allows code to be shared among articles
    *
    * @param state current Ids and configuration
    * @param node Sweble WtTemplate node
    * @return Template element plus any nested elements
    */
  private def processTemplate(state: WkpParserState, node: WtTemplate): List[WikipediaElement] = {

    val templateName = getTextFromNode(node.getName)
    val templateArgs = node.getArgs

    // Build Parameters
    def buildParameterPair(node: WtNode): (String, String) = {
      val name = getTextFromNode(node.get(0))
      val value = getTextFromNode(node.get(1))
      (name, value)
    }
    val parameterList = templateArgs.toList map buildParameterPair

    // Extract urls from parameters
    def innerNodeCheck(nvp: (String, String)): Boolean = Set("HTTP", "WWW.").exists(nvp._2.toUpperCase.startsWith)
    val linkNodes = parameterList
      .filter(innerNodeCheck)
      .map(_._2)
      .flatMap(x => processExternalLink(state, x, ""))

    // Retrieve nested nodes
    val innerNodes = parseNode(state, node).filter {
      case _:WikipediaText => false
      case _ => true}

    // Determine if template is an info box
    val isInfoBox = templateName.toUpperCase().startsWith("INFOBOX") || Set("TAXOBOX", "GEOBOX").contains(templateName.toUpperCase())

    List(WikipediaTemplate(state.articleId, state.headerId, state.elementIdItr.next, templateName, isInfoBox, parameterList)) ::: innerNodes ::: linkNodes
  }

  /** Wiki tables
    *
    * @param state current Ids and configuration
    * @param node Sweble WtTable node
    * @return HTML representation of the table.
    */
  private def processTable(state: WkpParserState, node: WtTable): List[WikipediaElement] = {

    // Get table caption
    def getTableCaption(node: WtNode): List[String] = node.toList flatMap {
      case n: WtTableCaption => List(getTextFromNode(n))
      case n: WtTableImplicitTableBody => getTableCaption(n)
      case n: WtBody => getTableCaption(n)
      case _ => List.empty
    }
    val caption = getTableCaption(node).mkString(" ")

    // Get HTML table
    def getHTML(table: String, node: WtNode): String = {
      val subResult = node.toList map {
        case n: WtTableRow => s"<tr>${getHTML(table, n)}</tr>"
        case n: WtTableHeader => s"<th>${getTextFromNode(n)}</th>"
        case n: WtTableCell => s"<td>${getTextFromNode(n)}</td>"
        case n: WtTableImplicitTableBody => getHTML(table, n)
        case n: WtBody => getHTML(table, n)
        case _ => ""
      }
      subResult.foldLeft(table)((tbl, nde) => tbl+ nde)
    }
    val html = s"<table>${ getHTML("", node).mkString("")}</table>"

    // Extract sub elements
    val innerList = parseNode(state, node).filter {
      case _:WikipediaText => false
      case _ => true}

    List(WikipediaTable(state.articleId, state.headerId, state.elementIdItr.next, caption, html)) :::  innerList
  }

  /** The text nodes are often buried deeply and frequently need to be retrieved.
    *
    * @param node Sweble WtNode (generic node)
    * @return Concatenated text and tag nodes
    */
  private def getTextFromNode(node: WtNode): String = node match {
      case n: WtText => n.getContent.replace("\r", "").replace("\n", "").trim
      case n: WtTagExtension => n.getBody.getContent
      case n: java.util.List[WtNode] if n.size() > 0 => n.map(getTextFromNode).mkString("")
      case _ => ""
  }
}
