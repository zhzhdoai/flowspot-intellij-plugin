package omni.scan.generate.sqlquery

object SQLQueryParser {
  def main(args: Array[String]): Unit = {
    val sql1 ="\"select  filerealpath from imagefile  where imagefileid=\" + str7"
      var query = sql1
        .stripPrefix("\"")
        .stripSuffix("\"")
        .split("\\\"\\s*\\+\\s*\\\"") // Splitting the query on '+' operator and joining back to form complete query
        .map(_.stripMargin)
        .mkString("")
    println(query)
    val sss = UtilitySQLParser.parseSqlQuery("\"select  filerealpath from imagefile  where imagefileid=\" + str7")
    println("123")
  }
}
