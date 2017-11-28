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
package main.scala.com.github.nielsenbe.sparkwikiparser.wikipedia

/** These classes represent the highly flattened abstract syntax tree for a wikipedia page.
  * Page
  * Page Redirect
  * * Header Section
  * * Text
  * * Template
  * * Template Parameters
  * * Link
  * * Tag
  * * Table
  */

/** Domain object for an Wikipedia page.
  * Structured representation of an pages meta data plus parsed wiki code.
  *
  * @param id Unique wikipedia ID from the dump file.
  * @param title Wikipedia page's title.
  * @param nameSpace Text name of a wiki's name space. https://en.wikipedia.org/wiki/Wikipedia:Namespace
  * @param pageType Unofficial classification of page type.  Some types must be heuristically inferred.
            ARTICLE, {NAMESPACE}, REDIRECT, DISAMBIGUATION, CATEGORY, LIST
  * @param lastRevisionId identifier for the last revision.
  * @param lastRevisionDate Date for when the page was last updated.
  */
case class WikipediaOutputPage(
  id: Long,
  title: String,
  nameSpace: String,
  pageType: String,
  lastRevisionId: Long,
  lastRevisionDate: Long)

/** Separate the redirect pages into their own file
  *
  * @param targetPageId Id of the page the redirect is redirecting to
  * @param redirectPageId Id of the page that contains the redirect
  * @param redirectTitle Title of the page that contains the redirect
  */
case class WikipediaOutputRedirect(
  targetPageId: Long,
  redirectPageId: Long,
  redirectTitle: String)

/** Container to hold header section data.
  *
  * @param parentPageId Wikimedia Id for the Page
  * @param headerId Unique (to the page) identifier for a header.
  * @param title Header text
  * @param level Header depth. 1 is Lead H2 = 2, H3 = 3, etc.
  * @param mainPage  A section's main_page.  This is derived from the main template. The main template
  *                     contains very important semantic information.
  * @param isAncillary If the header is a Reference, External Links, See more, etc type.
  */
case class WikipediaOutputHeader(
  parentPageId: Int,
  headerId: Int,
  title: String,
  level: Int,
  mainPage: Option[String],
  isAncillary: Boolean)

/** Natural language part of the wikipedia page.
  *
  * Natural text of an page.  The wikicode parsing process isn't an exact process and some artifacts
  * and some junk are to be expected.
  *
  * @param parentPageId Wikimedia Id for the Page
  * @param parentHeaderId The header the element is a child of.
  * @param text text fragment
  */
case class WikipediaOutputText (
  parentPageId: Int,
  parentHeaderId: Int,
  text: String)

/** Templates are a special MediaWiki construct that allows code to be shared among pages
  *
  * For example {{Global warming}} will create a table with links that are common to all GW related pages.
  *
  * @param parentPageId Wikimedia Id for the Page
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
  * @param templateType Template name, definition can be found via https://en.wikipedia.org/wiki/Template:[Template name]
  * @param isInfoBox Is the template part of the Infobox family
  */
case class WikipediaOutputTemplate(
  parentPageId: Int,
  parentHeaderId: Int,
  elementId: Int,
  templateType: String,
  isInfoBox: Boolean)

/** Templates can have 0 to many parameters
  *
  * @param parentPageId Wikimedia Id for the Page
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
  * @param paramName   If a argument is not named, then a place holder of *POS_[0 based index] is used.
  * @param paramValue Value of the parameter
  */
case class WikipediaOutputTemplateParameter(
  parentPageId: Int,
  parentHeaderId: Int,
  elementId: Int,
  paramName: String,
  paramValue: String
)

/** HTTP link to either an internal page or an external page.
  *
  * @param parentPageId Wikimedia Id for the Page
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
case class WikipediaOutputLink(
  parentPageId: Int,
  parentHeaderId: Int,
  elementId: Int,
  destination: String,
  text: String,
  linkType: String,
  subType: String,
  pageBookmark: String)

/** Contains info about an HTML tag.
  *
  * Special XML tags that are not handled else where in the code.
  * For the most part, ref and math are the main ones.
  *
  * @param parentPageId Wikimedia Id for the Page
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
  * @param tag tag name (without brackets)
  * @param tagValue contents inside of the tags
  */
case class WikipediaOutputTag (
  parentPageId: Int,
  parentHeaderId: Int,
  elementId: Int,
  tag: String,
  tagValue: String)

/** Contains info about a table.
  *
  * @param parentPageId Wikimedia Id for the Page
  * @param parentHeaderId The header the element is a child of.
  * @param elementId Unique (to the page) integer for an element.
  * @param caption Table title (if any).
  * @param html Table converted to HTML form.  Wiki tables are tricky to capture in a common
            structured form.  Columns and rows can be merged.  Table header tags can be abused.
            We default to leaving it in HTML and let the caller deal with it.
  */
case class WikipediaOutputTable (
  parentPageId: Int,
  parentHeaderId: Int,
  elementId: Int,
  caption: String,
  html: String)