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

  private def from(records: List[List[Any]]): List[TradSearchResult] =
    records.flatMap {
      case List(t, p, d, l) =>
        Option(
          TradSearchResult(t.toString, p.toString, d.toString, l.toString)
        )
      case _ => Option.empty[TradSearchResult]
    }

  def htmlTable(records: List[List[Any]]): String =
    TradSearchResult
      .from(records)
      .map { searchResult =>
        s"""
             |<tr>
             |  <td>${searchResult.`type`}</td>
             |  <td>${searchResult.price}</td>
             |  <td>${searchResult.description}</td>
             |  <td>${searchResult.link}</td>
             |</tr>
             |""".stripMargin
      }
      .mkString(
        """
          |<html>
          |<body>
          |<table>
          |   <tr>
          |       <th>Тип недвижимости</th>
          |       <th>Цена</th>
          |       <th>Описание</th>
          |       <th>Ссылка</th>
          |   </tr>
          |""".stripMargin,
        "",
        "</table></html>"
      )
}
