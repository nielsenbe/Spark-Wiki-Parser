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

