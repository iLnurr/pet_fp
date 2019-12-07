package an.custom

import an.model.{GetInfo, QueryInfo}

object queries {
  def constructQuery(queryInfo: QueryInfo): String =
    s"""
       |select '${queryInfo.houseInfo.houseType}' as houseType,
       |       t2.pagetitle as pagetitle,
       |       t1.price     as price,
       |       t2.uri       as link
       |from modx_ms2_products t1
       |         inner join modx_site_content t2
       |                    on t1.id = t2.id
       |where t1.rooms = ${queryInfo.houseInfo.roomsInt - 1}
       |  AND price > ${queryInfo.houseInfo.priceFrom - 1}
       |  AND price < ${queryInfo.houseInfo.priceTo + 1}
       |  AND t2.parent = ${mergeHouseType(queryInfo)}
       |""".stripMargin

  def mergeHouseType(queryInfo: QueryInfo): Int =
    (queryInfo.houseInfo.stage, queryInfo.houseInfo.houseType) match {
      case ("На стадии строительства", _) =>
        19
      case ("Новое готовое от застройщика", _) =>
        14
      case (_, "Квартира") =>
        13
      case (_, "Дом") =>
        11
      case (_, "Вилла") =>
        11
      case (_, "Отель") =>
        17
      case (_, "Участок") =>
        15
      case _ =>
        13
    }

  def constructQuery(getInfo: GetInfo): String = {
    import getInfo._
    val kvEqQ: String =
      kvEq.map { case (k, v) => k + "=" + s"'$v'" }.mkString("\n AND ")
    val kvMoreQ: String =
      kvMore.map { case (k, v) => k + ">" + v }.mkString("\n AND ")
    val kvLessQ: String =
      kvLess.map { case (k, v) => k + "<" + v }.mkString("\n AND ")

    val whereFragment =
      if (kvEq.nonEmpty || kvLess.nonEmpty || kvMore.nonEmpty)
        s"where \n$kvEqQ \n ${if (kvMoreQ.nonEmpty) s"AND $kvMoreQ" else ""} \n ${if (kvLessQ.nonEmpty)
          s"AND $kvLessQ"
        else ""}"
      else ""

    s"select ${fields.mkString(",")} from $tableName $whereFragment"
  }
}
