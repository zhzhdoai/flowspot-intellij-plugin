package omni.scan.generate

import io.joern.console.q
import omni.scan.*
import io.shiftleft.semanticcpg.language.*
import omni.scan.QueryMacros.withStrRep
import omni.rule.{SinkRule, SinkRuleParser}
import io.shiftleft.codepropertygraph.Cpg

/**
 * 从sinks.json文件生成对应的Query对象
 * 每个Query对象会被标记为sink，并包含pattern和taintedParams信息
 */
object SinkQueryGenerator extends QueryBundle {

  implicit val resolver: ICallResolver = NoResolve
  private val configBaseDir = "config"
  // 默认配置文件路径
  private val defaultSinkJsonPath = "config/sinks.json"
  
  /**
   * 生成所有sink规则对应的Query列表
   * @return Query列表
   */
  def generateSinkQueries(): List[Query] = {
    generateSinkQueries(defaultSinkJsonPath)
  }
  
  /**
   * 使用指定路径的sinks.json生成所有sink规则对应的Query列表
   * @param sinkJsonPath sinks.json文件路径（支持绝对路径和相对路径）
   * @return Query列表
   */
  def generateSinkQueries(sinkJsonPath: String): List[Query] = {
    try {
      // 加载sink规则
      val sinkRules = SinkRuleParser.loadRules(sinkJsonPath)
      
      // 为每个规则生成对应的Query
      val queries = sinkRules.flatMap(rule => createQueriesForRule(rule))
      
      queries
    } catch {
      case e: Exception =>
        // 如果加载失败，尝试使用默认路径
        if (sinkJsonPath != defaultSinkJsonPath) {
          generateSinkQueries()
        } else {
          List.empty
        }
    }
  }
  
  /**
   * 为单个SinkRule创建对应的Query列表
   * @param rule Sink规则
   * @return Query列表
   */
  private def createQueriesForRule(rule: SinkRule): List[Query] = {
    // 遍历每个sink模式
    rule.sinks.flatMap { sink =>
        // 处理每个pattern
        sink.patterns.zipWithIndex.map { case (pattern, index) =>
          // 提取方法模式和受污染的参数
          val (methodPattern, taintedParams) = extractPatternInfo(pattern)
          
          // 创建Query
          createSinkQuery(rule, methodPattern, taintedParams, index)
        }

    }
  }
  
  /**
   * 从pattern中提取方法模式和受污染的参数
   * @param pattern JSON值
   * @return (方法模式, 受污染的参数列表)
   */
  private def extractPatternInfo(pattern: org.json4s.JValue): (String, List[String]) = {
    import org.json4s.*
    
    pattern match {
      case JString(methodPattern) =>
        // 旧格式：只有方法名，默认所有参数都受污染
        (methodPattern, List.empty)
        
      case obj: JObject =>
        // 新格式：包含method和taintedParams字段
        val methodPattern = (obj \ "method").extract[String](org.json4s.DefaultFormats)
        
        val taintedParams = (obj \ "taintedParams").toOption match {
          case Some(JArray(params)) =>
            params.map(_.extract[String](org.json4s.DefaultFormats))
          case _ =>
            List.empty
        }
        
        (methodPattern, taintedParams.toList)
        
      case _ => ("", List.empty)
    }
  }
  
  /**
   * 创建单个Sink Query
   * @param rule Sink规则
   * @param methodPattern 方法模式
   * @param taintedParams 受污染的参数列表
   * @param index 模式索引
   * @return Query对象
   */
  @q
  private def createSinkQuery(rule: SinkRule, methodPattern: String, taintedParams: List[String], index: Int): Query = {
    val queryName = s"${rule.name}"
    val queryTitle = s"${rule.name}: ${methodPattern}"
    val queryDescription = s"""
      |${rule.description}
      |
      |Method Pattern: ${methodPattern}
      |Category: ${rule.category}
      |Priority: ${rule.priority}
      |Tainted Parameters: ${if (taintedParams.nonEmpty) taintedParams.mkString(", ") else "All parameters"}
      |""".stripMargin
    
    Query.make(
      name = queryName,
      author = "SinkQueryGenerator",
      title = queryTitle,
      description = queryDescription,
      score = rule.priority.toDouble,
      withStrRep({ cpg =>
        // 根据方法模式查找调用
      cpg.call.methodFullName(methodPattern).argument
          // 否则返回所有参数
        
      }),
      sinkPattern=methodPattern,
      category=rule.category,
      tags = List(QueryTags.taint, QueryTags.sink, QueryTags.default)
    )
  }
  
  /**
   * 生成所有sink查询
   * @return Query列表
   */
  def allSinkQueries(): List[Query] = {
    generateSinkQueries()
  }
  
  /**
   * 使用指定路径生成所有sink查询
   * @param sinkJsonPath sinks.json文件路径
   * @return Query列表
   */
  def allSinkQueries(sinkJsonPath: String): List[Query] = {
    generateSinkQueries(sinkJsonPath)
  }
}
