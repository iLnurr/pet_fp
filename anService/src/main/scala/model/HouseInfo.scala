package model

case class HouseInfo(
  price:     Long,
  region:    Option[String],
  rooms:     Int,
  houseType: String
)
