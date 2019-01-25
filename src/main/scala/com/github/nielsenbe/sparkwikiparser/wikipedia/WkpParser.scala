package com.github.nielsenbe.sparkwikiparser.wikipedia

/** Converts Wikitext to a simplified tree structure
  *
  * Wikipedia pages are coded using a special MediaWiki syntax.  Parsing this text is not a simple task and requires a
  * specialized parser.  In our case we use the Sweble wiki parser.  Sweble produces a deep and exact abstract syntax
  * tree of the wiki page.  For most purposes this is overkill.  This class takes the AST and transforms it into a cleaned
  * and simplified version.  The simplified tree classes can be found at:
  * main.scala.com.github.bnielsen.sparkwikiparser.wikipedia.wikipediaPageClasses
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
import scala.util.{Failure, Success, Try}

object WkpParser {
  /** Converts xml class to a simple abstract tree object
    *
    * Wraps the main parser in a try statement, returns a default page if invalid.
    *
    * @param input case class representation of xml element
    * @param wkpconfig parser options
    * @return WikipediaPage
    */
  def parseWiki(input: InputPage, wkpconfig: WkpParserConfiguration): WikipediaPage = {

    Try(parseWikiText(input, wkpconfig)) match {
      case Success(e) => e
      /* We have millions of pages to parse, don't stop for every error.  Log and move on. */
      case Failure(e) => WikipediaPage(
        input.id,
        input.title,
        "",
        0,
        0,
        0,
        "Error: " + e.getMessage,
        List.empty,
        List.empty,
        List.empty,
        List.empty,
        List.empty,
        List.empty)
    }
  }

  /** Converts xml class to a simple abstract tree object
    *
    * @param input case class representation of xml element
    * @param wkpconfig parser options
    * @return WikipediaPage
    */
  private def parseWikiText(input: InputPage, wkpconfig: WkpParserConfiguration): WikipediaPage = {

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

    /* Parse page */
    def parseWikiText(wikiText: String): List[WikipediaElement] = {
      val config = DefaultConfigEnWp.generate()
      val engine = new WtEngineImpl(config)
      val pageTitle = PageTitle.make(config, input.title)
      val pageId = new PageId(pageTitle, input.id)
      val cp = engine.postprocess(pageId, wikiText, null)
      val parsedPage = cp.getPage

      val state = new WkpParserState(input.id.intValue(), revId.intValue(), input.title, wkpconfig, engine, pageId)
      parseNode(state, parsedPage)
    }

    // Only parse if it is not a redirect or text is not empty
    val elements = redirect match {
      case "" if wikiText.length > 0 => parseWikiText(wikiText)
      case _ => List.empty
    }

    /* Group elements */
    val headers = List(WikipediaHeader(input.id.intValue, revId.intValue(), 0, "LEAD", 0)) ::: elements.collect { case n: WikipediaHeader => n }
    val templates = elements.collect { case n: WikipediaTemplate => n }
    val links = elements.collect { case n: WikipediaLink => n }
    val tags = elements.collect { case n: WikipediaTag => n }
    val tables = elements.collect { case n: WikipediaTable => n }

    /* Clean and prepare text */
    val text = elements
      .collect { case n: WikipediaText => n }
      .groupBy(_.parentHeaderId)
      .mapValues(x => x.map(_.text).mkString("").stripMargin('\n').trim)
      .map(x => WikipediaText(input.id.intValue(),revId.intValue(), x._1, x._2)).toList

    /* Return case class */
    WikipediaPage(
      input.id,
      input.title,
      redirect,
      input.ns,
      revId,
      lastRevision,
      "SUCCESS",
      headers,
      text,
      templates,
      links,
      tags,
      tables)
  }

  /** If Sweble doesn't correctly parse a section then we need send it back to the engine.
    *
    * @param state contains stateful information for processing nodes
    * @param wikiText text that needs to be re-parsed
    * @return List of our simplified nodes
    */
  private def parseWikiTextFragment(state: WkpParserState, wikiText: String): List[WikipediaElement] = {
    val cp = state.swebleEngine.postprocess(state.sweblePage, wikiText, null)
    val parsedFragment = cp.getPage
    parseNode(state, parsedFragment)
  }

  /** Determine how to parse each node type.
    *
    * @param state contains stateful information for processing nodes
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
    case n: WtOrderedList if state.config.parseTables => processList(state, n, "ol")
    case n: WtUnorderedList if state.config.parseTables => processList(state, n, "ul")
    case n: WtDefinitionList if state.config.parseTables => processList(state, n, "dl")
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
    * @param state contains stateful information for processing nodes
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

    List(WikipediaHeader(state.pageId, state.revisionId, headerId , headerName, level)) ::: nodes
  }

  /** Natural language text of an page.  What exactly constitutes this is determined by the parser.
    *
    * @param state contains stateful information for processing nodes
    * @param node Sweble Text node
    * @return Single text element (converted to a list for the recursive function)
    */
  private def processText(state: WkpParserState, node: WtText): List[WikipediaElement] = {
    // Standardize line returns
    val content = node.getContent.replace("\r", "\n")

    List(WikipediaText(state.pageId, state.revisionId, state.headerId, content ))
  }

  /** Internal wikimedia links
    *
    * @param state contains stateful information for processing nodes
    * @param node Sweble WtInternalLink  node
    * @return Link and its textual representation.  Links can and often are used as part of the text.
    */
  private def processWikiLink(state: WkpParserState, node: WtInternalLink): List[WikipediaElement] = {

    // Check if link is using bookmark [Page#SectionHeading]
    val hashSplit = node.getTarget.getAsString.split('#')
    val destination = hashSplit lift 0 match {
      case Some(dest) if dest == "" => state.title
      case Some(dest) => dest
      case None => ""
    }

    val pageBookmark = hashSplit lift 1 getOrElse ""

    // If title is empty, use destination
    val text = if(node.hasTitle) getTextFromNode(node.getTitle) else destination

    // Determine the link's namespace [Namespace:Page]
    val nameSpaces = Set("USER", "WIKIPEDIA", "FILE", "MEDIAWIKI", "TEMPLATE", "HELP","CATEGORY", "PORTAL", "BOOK")
    val leftSide = destination.split(':') lift 0 getOrElse ""
    val subType  = nameSpaces find(_ == leftSide.toUpperCase()) getOrElse "WIKIPEDIA"

    List(WikipediaLink(state.pageId, state.revisionId, state.headerId, state.elementIdItr.next, destination, text, "WIKIMEDIA", subType, pageBookmark)) :::
      List(WikipediaText(state.pageId, state.revisionId, state.headerId, text))
  }

  /** Internal Wikimedia image.
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

    List(WikipediaLink(state.pageId, state.revisionId, state.headerId, state.elementIdItr.next, destination, text, "WIKIMEDIA", "FILE", "")) :::
      List(WikipediaText(state.pageId, state.revisionId, state.headerId, text))
  }

  /** External link
    *
    * @param state contains stateful information for processing nodes
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
    // check for bo`okmark
    val hashSplit = destination.split('#')
    val cleanDest = hashSplit(0)
    val pageBookmark = hashSplit lift 1 getOrElse ""

    // For external links, the subtype is its domain.
    def parseURI(uri: String): Try[String] = Try(new URI(uri).getHost)
    val domain = parseURI(cleanDest).getOrElse("INVALID URI")

    List(WikipediaLink(state.pageId, state.revisionId, state.headerId, state.elementIdItr.next, cleanDest, title, "EXTERNAL", domain, pageBookmark))
  }

  /** HTML entities: > < & " ' etc
    * Convert them to text
    *
    * @param state contains stateful information for processing nodes
    * @param node Sweble WtXmlEntityRef node
    * @return text representation
    */
  private def processHTMLEntity(state: WkpParserState, node: WtXmlEntityRef): List[WikipediaElement] = {
    List(WikipediaText(state.pageId, state.revisionId, state.headerId, node.getResolved))
  }

  /** HTML elements such as < math > < / math > (spaces added due to docstring)
    *
    * @param state contains stateful information for processing nodes
    * @param node Sweble WtTagExtension node
    * @return Tag element plus any nested elements
    */
  private def processTagExtension(state: WkpParserState, node: WtTagExtension): List[WikipediaElement] = {
    val tagName = node.getName
    val tagBody = node.getBody.getContent

    // Retrieve links and templates from ref headers
    if(state.config.parseRefTags && tagName.toUpperCase() == "REF")
      parseWikiTextFragment(state, tagBody).filter {
        case _:WikipediaText => false
        case _ => true}
    else
      List(WikipediaTag(state.pageId, state.revisionId, state.headerId, state.elementIdItr.next, tagName, tagBody))
  }

  /** Templates are a special MediaWiki construct that allows code to be shared among pages
    *
    * @param state contains stateful information for processing nodes
    * @param node Sweble WtTemplate node
    * @return Template element plus any nested elements
    */
  private def processTemplate(state: WkpParserState, node: WtTemplate): List[WikipediaElement] = {

    val templateName = getTextFromNode(node.getName)
    val templateArgs = node.getArgs

    // Build Parameters
    def buildParameterPair(node: (WtNode, Int)): (String, String) = {
      val name = getTextFromNode(node._1.get(0)) match {case "" => "*POS_" + node._2 case n => n}
      val value = getTextFromNode(node._1.get(1))
      (name, value)
    }
    val parameterList = templateArgs
      .zipWithIndex
      .map(buildParameterPair)
      .toList

    // Extract urls from parameters
    def innerNodeCheck(nvp: (String, String)): Boolean = Set("HTTP", "WWW.").exists(nvp._2.toUpperCase.startsWith)
    val linkNodes = parameterList
      .filter(innerNodeCheck)
      .map(_._2)
      .flatMap(x => processExternalLink(state, x, x))

    // Retrieve nested nodes
    val innerNodes = parseNode(state, node).filter {
      case _:WikipediaText => false
      case _ => true}

    List(WikipediaTemplate(state.pageId, state.revisionId, state.headerId, state.elementIdItr.next, templateName, parameterList)) ::: innerNodes ::: linkNodes
  }

  /** Wiki tables
    *
    * @param state contains stateful information for processing nodes
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

    List(WikipediaTable(state.pageId, state.revisionId, state.headerId, state.elementIdItr.next, "TABLE", caption, html)) :::  innerList
  }

  /** This parse groups lists, ordered lists, and definition lists as tables.  They are converted to their
    * respective ul, ol, and dl html representations.
    *
    * @param state contains stateful information for processing nodes
    * @param node Sweble node of WtOrderedList, WtUnorderedList, and WtDefinitionList
    * @param nType OL, UL, or DL unordered list, ordered list, or definition list
    * @return Table element plus any nested nodes
    */
  private def processList(state: WkpParserState, node: WtContentNode, nType: String): List[WikipediaElement] = {

    def getListItemContents(node: WtNode): String = node match {
      case n: WtText => n.getContent.replace("\r", "").replace("\n", "").trim
      case n: WtTagExtension => n.getBody.getContent
      case n: WtInternalLink => n.map(getTextFromNode).mkString("")
      case n: WtOrderedList => ""
      case n: WtUnorderedList => ""
      case n: WtDefinitionList => ""
      case n: java.util.List[WtNode] if n.size() > 0 => n.map(getListItemContents).mkString("")
      case _ => ""
    }

    // Lists can be nested in other lists this causes some problems because the output needs to be
    // <ol><ol><li></li></ol></ol>
    // not <ol><li><li></li></li></ol>
    def getHTML(table: String, node: WtNode): String = {
      val subResult = node.toList map {
        case n: WtDefinitionListDef => s"<dd>${getListItemContents(n)}</dd>"
        case n: WtDefinitionListTerm => s"<dt>${getListItemContents(n)}</dt>"
        case n: WtListItem => s"<li>${getListItemContents(n)}</li>" + getHTML(table, n)
        case n: WtOrderedList => s"<ol>${getHTML(table, n)}</ol>"
        case n: WtUnorderedList => s"<ul>${getHTML(table, n)}</ul>"
        case n: WtDefinitionList => s"<dl>${getHTML(table, n)}</dl>"
        case _ => ""
      }
      subResult.foldLeft(table)((tbl, nde) => tbl+ nde)
    }
    val html = s"<$nType>${ getHTML("", node).mkString("")}</$nType>"

    // Extract sub elements
    val innerList = parseNode(state, node).filter {
      case _:WikipediaTable => false
      case _:WikipediaText => false
      case _ => true}

    List(WikipediaTable(state.pageId, state.revisionId, state.headerId, state.elementIdItr.next, nType.toUpperCase(), "", html)) :::  innerList
  }

  /** The text nodes are often buried deeply and frequently need to be retrieved.
    *
    * @param node Sweble WtNode (generic node)
    * @return Concatenated text and tag nodes
    */
  private def getTextFromNode(node: WtNode): String = node match {
    case n: WtText => n.getContent/*.replace("\r", "").replace("\n", "").trim*/
    case n: WtTagExtension => n.getBody.getContent
    case n: WtTemplate => ""
    case n: java.util.List[WtNode] if n.size() > 0 => n.map(getTextFromNode).mkString("")
    case _ => ""
  }
}