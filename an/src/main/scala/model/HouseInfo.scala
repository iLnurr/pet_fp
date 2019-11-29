package model

case class HouseInfo(
  stage:     String,
  houseType: String,
  rooms:     String,
  price:     String,
  date:      String,
  region:    Option[String]
) {
  private val curr = "€"
  private lazy val prices =
    price
      .split(" - ")
      .map(_.split(curr).last.toLong) // hardcoded price format in client like "€50000 - €100000"
  def priceFrom: Long = prices(0)
  def priceTo:   Long = prices(1)
}
