package com.github.nielsenbe.sparkwikiparser.wikipedia

/** Simple case classes to serialize a MediaWiki dump.
  * These should correspond one to one with the XML structure (for the elements we care about).
  */

/** Serialized form of XML */
case class InputRedirect(
  _VALUE: String,
  _title: String)

/** Serialized form of XML */
case class InputWikiText(
  _VALUE: String,
  _space: String)

/** Serialized form of XML */
case class InputRevision(
  timestamp: String,
  comment: String,
  format: String,
  id: Long,
  text: InputWikiText)

/** Serialized form of XML */
case class InputPage(
  id: Long,
  ns: Long,
  redirect: InputRedirect,
  restrictions: Option[String],
  title: String,
  revision: InputRevision)

