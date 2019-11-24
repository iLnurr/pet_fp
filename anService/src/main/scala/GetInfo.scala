case class GetInfo(
  tableName: String,
  fields:    Seq[String],
  kvEq:      Map[String, String],
  kvMore:    Map[String, String],
  kvLess:    Map[String, String]
)
