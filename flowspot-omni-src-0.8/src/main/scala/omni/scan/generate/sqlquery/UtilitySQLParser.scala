package omni.scan.generate.sqlquery

import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.create.table.CreateTable
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.{PlainSelect, Select}
import net.sf.jsqlparser.statement.update.Update
import omni.scan.generate.sqlquery.SQLQueryType.SQLQueryType

import scala.jdk.CollectionConverters.*
import scala.util.Try
import java.io.StringReader
import scala.util.matching.Regex

object UtilitySQLParser {
  def parseSqlQuery(sqlQuery: String): Option[List[SQLQuery]] = {
    try{
      val cleanedSql = SqlCleaner.clean(sqlQuery)
      val statement: Statement = CCJSqlParserUtil.parse(new StringReader(cleanedSql))
      statement match {
        case selectStmt: Select => {
          Some(
            Try(
              selectStmt.getWithItemsList.asScala
                .map(p => p.getSelect)
            ).toOption.getOrElse(List.empty[PlainSelect]).toList ++ List(selectStmt.getSelectBody)
          ).flatMap(p => SQLParser.createSQLNodesForSelect(p))
        }
        case _ =>
          None
      }
    } catch {
      case e: JSQLParserException =>
        println(s"Failed to parse the SQL query '$sqlQuery'. Error: ${e.getMessage}")
        None
      case e: Exception =>
        println(s"Failed to parse the SQL query '$sqlQuery'. Error: ${e.getMessage}")
        None
    }

  }

}
object SqlCleaner {
  def clean(sql: String): String = {
    var cleanedSql = removeComments(sql)
    cleanedSql = removeDynamicVariables(cleanedSql)
    cleanedSql.replace("`", "")
  }

  private def removeComments(sql: String): String = {
    // Replace /* ... */ style comments
    var cleanedSql = sql.replaceAll("/\\*.*?\\*/", "").trim

    // Replace -- style comments
    cleanedSql = cleanedSql.replaceAll("--.*", "")

    cleanedSql
  }

  private def removeDynamicVariables(sql: String): String = {
    // Replace :variable style dynamic variables
    val variablePattern = new Regex(":[a-zA-Z0-9_]+")
    variablePattern.replaceAllIn(sql, "")
  }
}
