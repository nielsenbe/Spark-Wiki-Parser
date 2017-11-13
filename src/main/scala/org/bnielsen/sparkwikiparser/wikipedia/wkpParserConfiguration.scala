package main.scala.org.bnielsen.sparkwikiparser.wikipedia

import org.sweble.wikitext.engine.{PageId, PageTitle, WtEngineImpl}
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp

case class ParserConfiguration(
  pageId: PageId,
  engine: WtEngineImpl
)

object ParserConfiguration {
  def getConfiguration: ParserConfiguration = {
    val config = DefaultConfigEnWp.generate
    val engine = new WtEngineImpl(config)
    val pageTitle = PageTitle.make(config, "DEFAULT")
    val pageId = new PageId(pageTitle, 1)

    ParserConfiguration(
      pageId,
      engine
    )
  }
}
