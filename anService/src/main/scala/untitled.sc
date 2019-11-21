import shapeless._
import shapeless.syntax._

val fields: Seq[String] = Seq("id2", "name", "next")
val kvEq:   Seq[(String, String)] = Seq("name" -> "test")
val kvMore: Seq[(String, String)] = Seq("id" -> "0", "id2" -> "2")
val kvLess: Seq[(String, String)] = Seq("id" -> "10", "id2" -> "4")

val kvEqQ = s"${kvEq.map{ case (k, v) => k + "=" + v}.mkString("\n AND ")}"
val kvMoreQ = s"${kvMore.map{ case (k, v) => k + ">" + v}.mkString("\n AND ")}"
val kvLessQ = s"${kvLess.map{ case (k, v) => k + "<" + v}.mkString("\n AND ")}"

val whereFragment = s"where \n$kvEqQ \n AND $kvMoreQ \n AND $kvLessQ"

val result = s"select ${fields.mkString(",")} from test $whereFragment"
