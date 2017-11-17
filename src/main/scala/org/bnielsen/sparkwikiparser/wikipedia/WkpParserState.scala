package main.scala.org.bnielsen.sparkwikiparser.wikipedia

class WkpParserState (
  val articleId: Int,
  val config: WkpParserConfiguration){
  var headerId: Int = 0
  val headerIdItr: Iterator[Int] = Stream.from(1).iterator
  val elementIdItr: Iterator[Int] = Stream.from(0).iterator
}
