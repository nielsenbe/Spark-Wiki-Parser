package main.scala.org.bnielsen.sparkwikiparser.wikipedia

case class WkpParserConfiguration(
  parseText: Boolean,
  parseTemplates: Boolean,
  parseLinks: Boolean,
  parseTags: Boolean,
  parseTables: Boolean
)
