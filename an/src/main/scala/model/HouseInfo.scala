package model

case class HouseInfo(
  stage:     String,
  houseType: String,
  rooms:     String,
  price:     String,
  date:      String
) {
  def extractNumbers(str: String): List[String] =
    str.split("\\D+").filter(_.nonEmpty).toList

  private lazy val prices = extractNumbers(price).map(_.toLong)

  def priceFrom: Long = prices.headOption.getOrElse(1L)
  def priceTo:   Long = prices.tail.headOption.getOrElse(Long.MaxValue)
  def roomsInt:  Int  = extractNumbers(rooms).headOption.map(_.toInt).getOrElse(1)
}
