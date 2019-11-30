package model

case class HouseInfo(
  stage:     String,
  houseType: String,
  rooms:     String,
  price:     String,
  date:      String
) {
  private val currencySymbol = "€"
  private lazy val prices =
    price
      .split(" - ")
      .map(_.split(currencySymbol).last.toLong) // hardcoded price format in quiz == "€50000 - €100000"
  def priceFrom: Long = prices(0)
  def priceTo:   Long = prices(1)
}
