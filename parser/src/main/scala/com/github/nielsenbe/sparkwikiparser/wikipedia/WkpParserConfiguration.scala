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

/** Settings for telling the parser which elements to parse
  *
  * @param parseText text can be turned off without impacting other elements
  * @param parseTemplates turning off templates will remove the ability to identify a sections main page
  *                       it will also cause some disambiguation pages to be labeled as articles
  * @param parseLinks turning off links will cause some errors in the text since links are often used as part of the text
  * @param parseTags turning off tags will cause many reference citations to disappear
  * @param parseTables tables can be turned off without impacting other elements
  * @param parseRefTags The Sweble parser does not extract reference templates from within < ref > tags
  *                     This means we need to re-parse the inner text.  This is a slow operation and can increase total parse time
  *                     by 0-100%.  If reference templates are not needed then it is recommended that his option be turned off
  */
case class WkpParserConfiguration(
  parseText: Boolean,
  parseTemplates: Boolean,
  parseLinks: Boolean,
  parseTags: Boolean,
  parseTables: Boolean,
  parseRefTags: Boolean
)
