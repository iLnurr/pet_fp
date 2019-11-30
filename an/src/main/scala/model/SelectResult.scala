package model

trait SelectResult

case class TradSearchResult(
  `type`:      String, //houseType
  price:       Long, //t1.price
  description: String, //t2.pageTitle + \n t2.longtitle
  link:        String //t2.uri
)
