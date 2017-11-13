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

/** These classes represent the simplified abstract syntax tree for a wikipedia article.
  * The goal behind these classes was to present a happy medium between a deep syntax tree and a completely flat
  * format.  The high level structure:
  * Article
  * * Header Section
  * * * Text
  * * * Template
  * * * Link
  * * * Tag
  * * * Table
  */

/** Common type for the wikipedia nodes
  */
sealed trait WikipediaElement

/** Domain object for an Wikipedia Article.
  * Structured representation of an articles meta data plus parsed wiki code.
  *
  * @param id Unique wikipedia ID from the dump file.
  * @param title Wikipedia article's title.
  * @param nameSpace Text name of a wiki's name space. https://en.wikipedia.org/wiki/Wikipedia:Namespace
  * @param pageType Unofficial classification of page type.  Some types must be heuristically inferred.
            ARTICLE, REDIRECT, DISAMBIGUATION, CATEGORY, LIST
  * @param lastRevisionId identifier for the last revision.
  * @param lastRevisionDate Date for when the article was last updated.
  * @param headerSections Flattened list of wikipedia header sections and their child nodes.
  */
case class WikipediaArticle(
  id: Long,
  title: String,
  redirect: String,
  nameSpace: String,
  pageType: String,
  lastRevisionId: Long,
  lastRevisionDate: Long,
  headerSections: List[WikipediaHeader],
  texts: List[WikipediaText],
  templates: List[WikipediaTemplate],
  links: List[WikipediaLink],
  tags: List[WikipediaTag],
  tables: List[WikipediaTable]
)

/** Container to hold header section data.
  *
  * @param index Unique (to the article) identifier for a header.
  * @param title Header text
  * @param level Header depth. 1 is Lead H2 = 2, H3 = 3, etc.
  * @param mainArticle  A section's main_article.  This is derived from the main template. The main template
  *                     contains very important semantic information.
  * @param elements Child elements (text, link, template, tag, table)
  */
case class WikipediaHeader(
  parentArticleId: Int,
  headerId: Int,
  title: String,
  level: Int,
  mainArticle: Option[String],
  isAncillary: Boolean) extends WikipediaElement

/** Natural language part of the wikipedia article.
  *
  * Natural text of an article.  The wikicode parsing process isn't an exact process and some artifacts
  * and some junk are to be expected.
  *
  * @param text text fragment
  */
case class WikipediaText (
  parentArticleId: Int,
  parentHeaderId: Int,
  text: String) extends WikipediaElement

/** Templates are a special MediaWiki construct that allows code to be shared among articles
  *
  * For example {{Global warming}} will create a table with links that are common to all GW related articles.
  *
  * @param templateType Template name, definition can be found via https://en.wikipedia.org/wiki/Template:[Template name]
  * @param parameters Templates can have 0..n parameters.  These may be named (arg=val) or just  referenced sequentially.
  *                   In this code they are represented via list of tuple (arg, value).
  */
case class WikipediaTemplate(
  parentArticleId: Int,
  parentHeaderId: Int,
  templateType: String,
  isInfoBox: Boolean,
  parameters: List[(String, String)]) extends WikipediaElement

/** HTTP link to either an internal page or an external page.
  *
  * @param destination URL.  For internal links, the wikipedia title, otherwise the domain.
            Internal domains may (and often do) point to redirects.  This needs to be taken
            into account when analysing links.
  * @param text The textual overlay for a link.  If empty the destination will be used.
  * @param linkType WIKIMEDIA or EXTERNAL
  * @param subType Namespace for WIKIMEDIA links or the domain for external links
  * @param pageBookmark We separate the page book mark from the domain for analytic purposes.
            www.test.com#page_bookmark becomes www.test.com  and page_bookmark.
  */
case class WikipediaLink(
  parentArticleId: Int,
  parentHeaderId: Int,
  destination: String,
  text: String,
  linkType: String,
  subType: String,
  pageBookmark: String) extends WikipediaElement

/** Contains info about an HTML tag.
  *
  * We exclude some tags like ref and reflist because their contents are found elsewhere.
  *
  * @param tag tag name (without brackets)
  * @param tagValue contents inside of the tags
  */
case class WikipediaTag (
  parentArticleId: Int,
  parentHeaderId: Int,
  tag: String,
  tagValue: String) extends WikipediaElement

/** Contains info about a table.
  *
  * @param caption Table title (if any).
  * @param html Table converted to HTML form.  Wiki tables are tricky to capture in a common
            structured form.  Columns and rows can be merged.  Table header tags can be abused.
            We default to leaving it in HTML and let the caller deal with it.
  */
case class WikipediaTable (
  parentArticleId: Int,
  parentHeaderId: Int,
  caption: String,
  html: String) extends WikipediaElement