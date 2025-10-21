package omni.scan.generate.sqlquery

import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.expression.{BinaryExpression, CastExpression, Function}
import net.sf.jsqlparser.parser.{ASTNodeAccess, CCJSqlParserUtil}
import net.sf.jsqlparser.schema.Table
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.create.table.CreateTable
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.{ParenthesedSelect, PlainSelect, Select, SelectItem, SetOperationList}
import net.sf.jsqlparser.statement.update.Update
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.*
import java.io.StringReader
import scala.util.Try
import scala.util.control.Breaks.{break, breakable}

object SQLParser {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val NUMBER_ONE      = 1
  val NUMBER_MINUSONE = -1

  @deprecated
  def createSQLNodesForSelect(selectStmts: List[Select]): Option[List[SQLQuery]] = {
    Some(selectStmts.flatMap {
      case plainSelect: PlainSelect if plainSelect.getFromItem.isInstanceOf[Table] =>
        val sqlTable = createSQLTableItem(plainSelect.getFromItem.asInstanceOf[Table])
        List(SQLQuery(SQLQueryType.SELECT, sqlTable, getColumns(plainSelect, sqlTable)))
      case plainSelect: PlainSelect if plainSelect.getFromItem.isInstanceOf[ParenthesedSelect] =>
        createSQLNodesForSelect(List(plainSelect.getFromItem.asInstanceOf[ParenthesedSelect].getSelect))
          .getOrElse(List.empty[SQLQuery])
      case parenthesedSelect: ParenthesedSelect =>
        createSQLNodesForSelect(List(parenthesedSelect.getSelect)).getOrElse(List.empty[SQLQuery])
      /*
         Example of SetOperation SQL Queries:
        -- SELECT column_name FROM table1
        -- UNION|INTERSECT
        -- SELECT column_name FROM table2;
       */
      case setOpList: SetOperationList =>
        val selectStmts = setOpList.getSelects.asScala.toList
        val tableNameColumnListMap = selectStmts.map { stmt =>
          val plainSelect = stmt.asInstanceOf[PlainSelect]
          val sqlTable    = createSQLTableItem(plainSelect.getFromItem.asInstanceOf[Table])
          (sqlTable, getColumns(plainSelect, sqlTable))
        }

        // Merge all column lists into a single list of unique columns
        tableNameColumnListMap.map((i) => {
          SQLQuery(SQLQueryType.SELECT, i._1, i._2)
        })
    })
  }

  def parseSqlQuery(sqlQuery: String): Option[List[SQLQuery]] = {
    try {
      val cleanedSql           = SqlCleaner.clean(sqlQuery)
      val statement: Statement = CCJSqlParserUtil.parse(new StringReader(cleanedSql))
      statement match {
        case selectStmt: Select => {
          Some(
            Try(
              selectStmt.getWithItemsList.asScala
                .map(p => p.getSelect)
            ).toOption.getOrElse(List.empty[PlainSelect]).toList ++ List(selectStmt.getSelectBody)
          ).flatMap(p => createSQLNodesForSelect(p))
        }
        case insertStmt: Insert =>
          val sqlTable = createSQLTableItem(insertStmt.getTable)
          Some(
            List(
              SQLQuery(
                SQLQueryType.INSERT,
                sqlTable,
                insertStmt.getColumns.asScala.map(x => createSQLColumnItem(x, sqlTable)).toList
              )
            )
          )
        case updateStmt: Update =>
          val sqlTable = createSQLTableItem(updateStmt.getTable)
          Some(
            List(
              SQLQuery(
                SQLQueryType.UPDATE,
                sqlTable,
                updateStmt.getColumns.asScala.map(x => createSQLColumnItem(x, sqlTable)).toList
              )
            )
          )
        case createStmt: CreateTable =>
          val columns: List[String] =
            createStmt.getColumnDefinitions.asScala.map(_.getColumnName).toList

          val sqlTable = createSQLTableItem(createStmt.getTable)
          val columnList = columns.map(columnName => {
            val lineColumn = getLineAndColumnNumber(sqlQuery, columnName)
            SQLColumn(columnName, lineColumn._1, lineColumn._2)
          })
          Some(List(SQLQuery(SQLQueryType.CREATE, sqlTable, columnList)))
        case _ =>
          logger.debug("Something wrong: ", sqlQuery)
          None
      }
    } catch {
      case e: JSQLParserException =>
        logger.debug(s"Failed to parse the SQL query '$sqlQuery'. Error: ${e.getMessage}")
        None
      case e: Exception =>
        logger.debug(s"Failed to parse the SQL query '$sqlQuery'. Error: ${e.getMessage}")
        None
    }
  }

  def getColumns(plainSelect: PlainSelect, sqlTable: SQLTable): List[SQLColumn] = {
    plainSelect.getSelectItems.asScala.flatMap { (item: SelectItem[?]) =>
      item.toString match {
        case f: String if f.contains("(") =>
          val parsedResult = CCJSqlParserUtil.parseExpression(f)
          parsedResult match
            case function: Function =>
              function.getParameters.getExpressions.asScala.map(column => {
                createSQLColumnItem(column, sqlTable)
              })
            case castExpression: CastExpression =>
              List(createSQLColumnItem(castExpression.getLeftExpression, sqlTable))
            case binaryExpr: BinaryExpression => List(createSQLColumnItem(binaryExpr.getLeftExpression, sqlTable))
            case _                            => List(createSQLColumnItem(item, sqlTable))
        case _ =>
          List(createSQLColumnItem(item, sqlTable))
      }
    }.toList
  }

  private def createSQLTableItem(table: Table): SQLTable = {
    val tableName: String = table.getName
    val tableLineNumber   = Try(table.getASTNode.jjtGetFirstToken().beginLine).getOrElse(NUMBER_ONE)
    val tableColumnNumber = Try(table.getASTNode.jjtGetFirstToken().beginColumn).getOrElse(NUMBER_MINUSONE)
    SQLTable(tableName, tableLineNumber, tableColumnNumber)
  }

  private def createSQLColumnItem(column: ASTNodeAccess, sqlTable: SQLTable) = {
    SQLColumn(
      column.toString,
      Try(column.getASTNode.jjtGetFirstToken().beginLine).getOrElse(NUMBER_ONE),
      Try(column.getASTNode.jjtGetFirstToken().beginColumn).getOrElse(NUMBER_MINUSONE)
    )
  }

  private def getLineAndColumnNumber(sqlQuery: String, columnName: String) = {
    var foundLineNumber   = NUMBER_ONE
    var foundColumnNumber = NUMBER_MINUSONE
    breakable {
      sqlQuery.split("\n").zipWithIndex.foreach { case (queryLine, lineNumber) =>
        val columnNumber = queryLine.indexOf(columnName)
        if (columnNumber != NUMBER_MINUSONE) {
          foundLineNumber = lineNumber + NUMBER_ONE
          foundColumnNumber = columnNumber
          break()
        }
      }
    }
    (foundLineNumber, foundColumnNumber)
  }

}
