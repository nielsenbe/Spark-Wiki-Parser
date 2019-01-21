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
package com.github.nielsenbe.sparkwikiparser.wikipedia

/** These classes represent the simplified abstract syntax tree for a wikipedia page.
  * The goal behind these classes was to flatten the initial deep abstract syntax tree into something more manageable.
  * Article
  * * Header Section
  * * Text
  * * Template
  * * Link
  * * Tag
  * * Table
  */

/** Common type for the wikipedia nodes
  */
sealed trait WikipediaElement

/** Domain object for an Wikipedia page.
  * Structured representation of a page's meta data plus parsed wiki code.
  *
  * @param id Unique wikipedia ID from the dump file.
  * @param title Wikipedia page's title.
  * @param nameSpace Text name of a wiki's name space. https://en.wikipedia.org/wiki/Wikipedia:Namespace
  * @param pageType Similar to namespace, but Articles are split into ARTICLE, REDIRECT, DISAMBIGUATION, and LIST.
  *                 Otherwise defaults to namespace.
  * @param revisionId identifier for the last revision.
  * @param revisionDate Date for when the page was last updated.
  * @param parserMessage SUCCESS or the error message
  * @param headerSections Flattened list of wikipedia header sections
  * @param texts Natural language portion of page
  * @param templates WikiMedia templates
  * @param links Wikimedia and Exteranl Links
  * @param tags Handful of extended tags
  * @param tables Wikimedia tables converted to HTML
  */
case class WikipediaPage(
  id: Long,
  title: String,
  redirect: String,
  nameSpace: Long,
  revisionId: Long,
  revisionDate: Long,
  parserMessage: String,
  headerSections: List[WikipediaHeader],
  texts: List[WikipediaText],
  templates: List[WikipediaTemplate],
  links: List[WikipediaLink],
  tags: List[WikipediaTag],
  tables: List[WikipediaTable])

/** Container to hold header section data.
  *
  * @param parentPageId Wikimedia Id for the page
  * @param parentRevisionId Revision Id element is associated with
  * @param headerId Unique (to the page) identifier for a header.
  * @param title Header text
  * @param level Header depth. 1 is Lead H2 = 2, H3 = 3, etc.
  */
case class WikipediaHeader(
  parentPageId: Int,
  parentRevisionId: Int,
  headerId: Int,
  title: String,
  level: Int) extends WikipediaElement

/** Natural language part of the wikipedia page.
  *
  * Natural text of an page.  The wikicode parsing process isn't an exact process and some artifacts
  * and some junk are to be expected.
  *
  * @param parentPageId Wikimedia Id for the Page
  * @param parentRevisionId Revision Id element is associated with
  * @param parentHeaderId The header the element is a child of.
  * @param text text fragment
  */
case class WikipediaText (
  parentPageId: Int,
  parentRevisionId: Int,
  parentHeaderId: Int,
  text: String) extends WikipediaElement

/** Templates are a special MediaWiki construct that allows code to be shared among pages
  *
  * For example {{Global warming}} will create a table with links that are common to all GW related pages.
  *
  * @param parentPageId Wikimedia Id for the page
  * @param parentRevisionId Revision Id element is associated with
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
  * @param templateType Template name, definition can be found via https://en.wikipedia.org/wiki/Template:[Template name]
  * @param parameters Templates can have 0..n parameters.  These may be named (arg=val) or just  referenced sequentially.
  *                   In this code they are represented via list of tuple (arg, value).
  *                   If a argument is not named, then a place holder of *POS_[0 based index] is used.
  */
case class WikipediaTemplate(
  parentPageId: Int,
  parentRevisionId: Int,
  parentHeaderId: Int,
  elementId: Int,
  templateType: String,
  parameters: List[(String, String)]) extends WikipediaElement

/** HTTP link to either an internal page or an external page.
  *
  * @param parentPageId Wikimedia Id for the page
  * @param parentRevisionId Revision Id element is associated with
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
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
  parentPageId: Int,
  parentRevisionId: Int,
  parentHeaderId: Int,
  elementId: Int,
  destination: String,
  text: String,
  linkType: String,
  subType: String,
  pageBookmark: String) extends WikipediaElement

/** Contains info about an HTML tag.  Mostly these are tags that Sweble cannot parse.
  *
  * Special XML tags that are not handled else where in the code.
  * For the most part, ref and math are the main ones.
  *
  * @param parentPageId Wikimedia Id for the page
  * @param parentRevisionId Revision Id element is associated with
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
  * @param tag tag name (without brackets)
  * @param tagValue contents inside of the tags
  */
case class WikipediaTag (
  parentPageId: Int,
  parentRevisionId: Int,
  parentHeaderId: Int,
  elementId: Int,
  tag: String,
  tagValue: String) extends WikipediaElement

/** Contains info about a table.
  *
  * @param parentPageId Wikimedia Id for the page
  * @param parentRevisionId Revision Id element is associated with
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
  * @param tableHtmlType The primary html element of the table TABLE, OL, UL, or DL
  * @param caption Table title (if any).
  * @param html Table converted to HTML form.  Wiki tables are tricky to capture in a common
            structured form.  Columns and rows can be merged.  Table header tags can be abused.
            We default to leaving it in HTML and let the caller deal with it.
  */
case class WikipediaTable (
  parentPageId: Int,
  parentRevisionId: Int,
  parentHeaderId: Int,
  elementId: Int,
  tableHtmlType: String,
  caption: String,
  html: String) extends WikipediaElement