package model

case class HouseInfo(
  stage:     String,
  houseType: String,
  rooms:     String,
  price:     String,
  date:      String,
  region:    Option[String]
)
