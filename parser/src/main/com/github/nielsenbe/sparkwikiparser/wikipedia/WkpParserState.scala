package com.github.nielsenbe.sparkwikiparser.wikipedia

import org.sweble.wikitext.engine.{PageId, WtEngineImpl}

class WkpParserState (
  val pageId: Int,
  val revisionId: Int,
  val title: String,
  val config: WkpParserConfiguration,
  val swebleEngine: WtEngineImpl,
  val sweblePage: PageId
){
  var headerId: Int = 0
  val headerIdItr: Iterator[Int] = Stream.from(1).iterator
  val elementIdItr: Iterator[Int] = Stream.from(0).iterator
}
