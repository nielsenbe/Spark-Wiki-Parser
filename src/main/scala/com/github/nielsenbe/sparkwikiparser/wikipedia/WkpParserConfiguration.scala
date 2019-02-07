package com.github.nielsenbe.sparkwikiparser.wikipedia

case class WkpParserConfiguration(
  parseText: Boolean,
  parseTemplates: Boolean,
  parseLinks: Boolean,
  parseTags: Boolean,
  parseTables: Boolean,
  parseRefTags: Boolean
)
