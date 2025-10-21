package omni.scan.generate.sqlquery

import omni.scan.generate.sqlquery.SQLQueryType.SQLQueryType
case class SQLTable(name: String, lineNumber: Int, columnNumber: Int)

case class SQLColumn(name: String, lineNumber: Int, columnNumber: Int)

case class SQLQuery(queryType: SQLQueryType, table: SQLTable, column: List[SQLColumn])

object SQLQueryType extends Enumeration {
  type SQLQueryType = String

  val SELECT = "SELECT"
  val INSERT = "INSERT"
  val UPDATE = "UPDATE"
  val DELETE = "DELETE"
  val DROP   = "DROP"
  val CREATE = "CREATE"

}
