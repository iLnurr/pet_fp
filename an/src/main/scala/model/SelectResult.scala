package model

trait SelectResult

case class TradSearchResult(
  `type`:      String, //houseType
  price:       String, //t1.price
  description: String, //t2.pageTitle + \n t2.longtitle
  link:        String //t2.uri
) extends SelectResult

object TradSearchResult {
  val heads = List("Тип недвижимости", "Цена", "Описание", "Ссылка")
}
