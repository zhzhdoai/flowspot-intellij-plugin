package omni.rule

import org.json4s.*
import org.json4s.native.JsonMethods.*

import scala.io.Source
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.CfgNode
import io.shiftleft.semanticcpg.language.*
import java.util.regex.Pattern
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

// 添加注解以防止混淆
@SerialVersionUID(1L)
case class SinkRule(
                     name: String,
                     typeName: String,
                     priority: Int,
                     category: String,
                     description: String,
                     sinks: List[Sink]
                   )

@SerialVersionUID(1L)
case class SinkCategory(
                         name: String,
                         displayName: String,
                         description: String,
                         priority: Int,
                         rules: List[SinkRule]
                       )

@SerialVersionUID(1L)
case class Sink(
                 typeName: String,
                 patterns: List[JValue] // 使用JValue以支持不同格式
               )

object SinkRuleParser {
  implicit val formats: DefaultFormats.type = DefaultFormats

  def loadRules(sinkFile: String): List[SinkRule] = {
    try {
      // 读取文件内容
      val source = Source.fromFile(sinkFile, "UTF-8")
      val content = source.mkString
      source.close()
      
      // 解析JSON，支持扁平数组格式
      val json = parse(content)
      json match {
        case JArray(rules) => 
          // 扁平数组格式：直接解析规则数组
          rules.map(parseRule).filter(_ != null)
        case obj: JObject =>
          // 分组格式：包含categories字段（向后兼容）
          val categoriesJson = obj \ "category"
          categoriesJson match {
            case JArray(categories) =>
              categories.map(parseCategory).filter(_ != null).flatMap(_.rules)
            case _ =>
              List.empty[SinkRule]
          }
        case _ => 
          List.empty[SinkRule]
      }
    } catch {
      case e: Exception =>
        List.empty[SinkRule]
    }
  }
  
  def loadCategories(sinkFile: String): List[SinkCategory] = {
    try {
      // 读取文件内容
      val source = Source.fromFile(sinkFile, "UTF-8")
      val content = source.mkString
      source.close()
      
      // 解析分组结构
      val json = parse(content)
      json match {
        case obj: JObject =>
          val categoriesJson = obj \ "category"
          categoriesJson match {
            case JArray(categories) =>
              categories.map(parseCategory).filter(_ != null)
            case _ =>
              List.empty[SinkCategory]
          }
        case _ => 
          List.empty[SinkCategory]
      }
    } catch {
      case e: Exception =>
        List.empty[SinkCategory]
    }
  }
  
  // 手动解析单个分组
  private def parseCategory(json: JValue): SinkCategory = {
    try {
      val name = (json \ "name").extract[String]
      val displayName = (json \ "displayName").extract[String]
      val description = (json \ "description").extract[String]
      val priority = (json \ "priority").extract[Int]
      
      // 解析rules数组
      val rulesJson = json \ "rules"
      val rules = rulesJson match {
        case JArray(rulesList) => 
          rulesList.map(parseRule).filter(_ != null)
        case _ => List.empty[SinkRule]
      }
      
      SinkCategory(name, displayName, description, priority, rules)
    } catch {
      case e: Exception =>
        null
    }
  }
  
  // 手动解析单个规则
  private def parseRule(json: JValue): SinkRule = {
    try {
      val name = (json \ "name").extract[String]
      val typeName = (json \ "typeName").extract[String]
      val priority = (json \ "priority").extract[Int]
      val category = (json \ "category").extract[String]
      val description = (json \ "description").extract[String]
      
      // 解析sinks数组
      val sinksJson = json \ "sinks"
      val sinks = sinksJson match {
        case JArray(sinksList) => 
          sinksList.map(parseSink).filter(_ != null)
        case _ => List.empty[Sink]
      }
      
      SinkRule(name, typeName, priority, category, description, sinks)
    } catch {
      case e: Exception =>
        null
    }
  }
  
  // 手动解析单个sink
  private def parseSink(json: JValue): Sink = {
    try {
      val typeName = (json \ "typeName").extract[String]
      
      // 解析patterns数组
      val patternsJson = json \ "patterns"
      val patterns = patternsJson match {
        case JArray(patternsList) => patternsList
        case _ => List.empty[JValue]
      }
      
      Sink(typeName, patterns)
    } catch {
      case e: Exception =>
        null
    }
  }

  
  /**
   * 获取每个单独的模式和对应的节点
   * @param cpg CPG对象
   * @param rule Sink规则
   * @return 模式到对应节点列表的映射
   */
  def getSinkNodesByPattern(cpg: Cpg, rule: SinkRule): Map[String, List[CfgNode]] = {
    val result = for {
      sinkPattern <- rule.sinks
      if sinkPattern.typeName == "CALL"
      pattern <- sinkPattern.patterns
    } yield {
      val patternStr = pattern match {
        case JString(methodPattern) => methodPattern
        case obj: JObject => (obj \ "method").extract[String]
        case _ => ""
      }

      val nodes = cpg.call.methodFullName(patternStr).argument.toList

      patternStr -> nodes
    }

    result.toMap
  }

}

