package omni.test

import io.joern.dataflowengineoss.language.*
import io.shiftleft.codepropertygraph.cpgloading.CpgLoader
import io.shiftleft.codepropertygraph.generated.{Cpg, DiffGraphBuilder}
import io.shiftleft.codepropertygraph.generated.nodes.*

import scala.jdk.CollectionConverters.*
import io.shiftleft.semanticcpg.language.*

import scala.language.postfixOps
import scala.collection.mutable.ListBuffer
import scala.util.Try
import scala.util.chaining.scalaUtilChainingOps

object SpringApiFilter {
  // 支持的HTTP方法注解类型
  private val MAPPING_ANNOTATIONS = Set(
    "RequestMapping", "GetMapping", "PostMapping",
    "PutMapping", "DeleteMapping", "PatchMapping"
  )
  

  /**
   * 加载CPG图
   */
  private def loadCpg(path: String): Cpg = {
    Try(CpgLoader.load(path)).getOrElse {
      throw new RuntimeException(s"Failed to load CPG from $path")
    }
  }

  /**
   * 提取所有Controller的Mapping信息
   */
  def extractAllMappings(cpg: Cpg): List[ControllerMapping] = {
    val result = ListBuffer[ControllerMapping]()

    // 查找所有Controller类
    cpg.typeDecl
      .where(_.annotation.name(".*Controller$|.*Path$"))
      .foreach { controller =>
        val controllerMappings = extractControllerMappings(controller)
        result ++= controllerMappings
      }

    result.toList
  }

  /**
   * 提取单个Controller的所有Mapping
   */
  private def extractControllerMappings(controller: TypeDecl): List[ControllerMapping] = {
    val mappings = ListBuffer[ControllerMapping]()
    val controllerPath = getControllerBasePath(controller)

    // 处理每个方法
    controller.method.foreach { method =>
      method.annotation
        .name(".*Mapping$|.*Path$")
        .foreach { annotation =>
          val mapping = buildMappingInfo(controller, method, annotation, controllerPath)
          mappings += mapping
        }
    }

    mappings.toList
  }

  /**
   * 构建完整的Mapping信息
   */
  private def buildMappingInfo(
                                controller: TypeDecl,
                                method: Method,
                                annotation: Annotation,
                                controllerPath: String
                              ): ControllerMapping = {
    val methodPath = getAnnotationPathValue(annotation)
    val fullPath = normalizePath(controllerPath, methodPath)
    val httpMethod = resolveHttpMethod(annotation.name, annotation)
    val consumes = getAnnotationAttribute(annotation, "consumes")
    val produces = getAnnotationAttribute(annotation, "produces")
    val params = extractMethodParameters(method)

    ControllerMapping(
      controllerFullName = controller.fullName,
      controllerSimpleName = controller.name,
      methodName = method.name,
      httpMethod = httpMethod,
      path = fullPath,
      consumesMediaTypes = consumes,
      producesMediaTypes = produces,
      parameters = params
    )
  }

  /**
   * 获取Controller的基础路径
   */
  private def getControllerBasePath(controller: TypeDecl): String = {
    controller.annotation
      .name(".*Mapping$")
      .map(getAnnotationPathValue)  // 直接映射，因为getAnnotationPathValue内部已经处理了异常情况
      .find(_.nonEmpty)
      .getOrElse("")
  }

  /**
   * 从注解中提取路径值
   */
  private def getAnnotationPathValue(annotation: Annotation): String = {
    // 尝试从value或path参数获取
    val valuePath = annotation.astChildren
      .collect {
        case param: AnnotationParameterAssign
           =>
          // 处理多种格式的注解值
          val pattern = """"([^"]*)"""".r
          pattern.findFirstMatchIn(param.code) match {
            case Some(m) => m.group(1)
            case None => ""
          }
      }
      .find(_.nonEmpty)
    // 如果value/path参数没有值，尝试从注解名称中提取
    val namePath = if (valuePath.isEmpty) {
      annotation.name.split("\\.").last match {
        case name if name.endsWith("Mapping") =>
          // 尝试从注解名称中提取路径
          val path = name.replace("Mapping", "").toLowerCase
          if (path.nonEmpty) Some(path) else None
        case _ => None
      }
    } else None
    val namePathNone = Some("")
    // 返回找到的第一个有效路径
    valuePath.orElse(namePath).getOrElse("")
  }

  /**
   * 获取注解的其他属性值
   */
  private def getAnnotationAttribute(annotation: Annotation, attrName: String): List[String] = {
    annotation.astChildren
      .collect {
        case param: AnnotationParameterAssign if param.code == attrName =>
          param.astOut.collect {
            case literal: AnnotationLiteral =>
              literal.code.stripPrefix("\"").stripSuffix("\"")
          }
      }
      .flatten
      .toList
  }

  /**
   * 解析HTTP方法
   */
  private def resolveHttpMethod(annotationName: String, annotation: Annotation): String = {
    annotationName.split("\\.").last match {
      case name if MAPPING_ANNOTATIONS.contains(name) =>
        name.replace("Mapping", "").toUpperCase
      case _ =>
        getAnnotationAttribute(annotation, "method").headOption
          .map(_.toUpperCase)
          .getOrElse("GET") // 默认GET方法
    }
  }

  /**
   * 提取方法参数信息
   */
  private def extractMethodParameters(method: Method): List[MethodParameter] = {
    method.parameter.map { param =>
      val paramName = param.name
      val paramType = param.typeFullName
      val annotations = param.annotation.map(_.name).toList

      // 检测常见注解
      val isPathVariable = annotations.exists(_.contains("PathVariable"))
      val isRequestParam = annotations.exists(_.contains("RequestParam"))
      val isRequestBody = annotations.exists(_.contains("RequestBody"))

      MethodParameter(
        name = paramName,
        typeName = paramType,
        isPathVariable = isPathVariable,
        isRequestParam = isRequestParam,
        isRequestBody = isRequestBody
      )
    }.toList
  }

  /**
   * 规范化路径
   */
  private def normalizePath(controllerPath: String, methodPath: String): String = {
    val combined = if (controllerPath.isEmpty) {
      methodPath
    } else if (methodPath.isEmpty) {
      controllerPath
    } else {
      s"$controllerPath/${methodPath.stripPrefix("/")}"
    }

    // 标准化路径格式
    combined
      .replaceAll("/+", "/")
      .replaceAll("^/|/$", "")
      .pipe(p => if (p.isEmpty) "/" else s"/$p")
  }

  /**
   * 打印Mapping信息
   */
  private def printMappings(mappings: List[ControllerMapping]): Unit = {
    mappings.foreach { m =>
      println(
        s"""
           |Controller: ${m.controllerSimpleName} (${m.controllerFullName})
           |Method: ${m.httpMethod} ${m.path}
           |Handler: ${m.methodName}
           |Consumes: ${m.consumesMediaTypes.mkString(", ")}
           |Produces: ${m.producesMediaTypes.mkString(", ")}
           |Parameters:
           |${m.parameters.map(p => s"  - ${p.name}: ${p.typeName}${if (p.isPathVariable) " (PATH)" else ""}").mkString("\n")}
           |""".stripMargin)
    }
  }

  private def printMappings2(mappings: List[ControllerMapping]): Unit = {
    mappings.foreach { m =>
      println(
        s"""
           |Controller: ${m.controllerFullName}#${m.methodName}
           |PATH: ${m.httpMethod} ${m.path}
           |""".stripMargin)
    }
  }


  /**
   * 生成OpenAPI格式
   */
  private def generateOpenApi(mappings: List[ControllerMapping]): String = {
    val pathsJson = mappings.groupBy(_.path).map { case (path, methods) =>
      s""""$path": {
         |${methods.map(methodToOpenApi).mkString(",\n")}
         |}""".stripMargin
    }.mkString(",\n")

    s"""{
       |  "openapi": "3.0.0",
       |  "paths": {
       |$pathsJson
       |  }
       |}""".stripMargin
  }

  private def methodToOpenApi(mapping: ControllerMapping): String = {
    val parameters = mapping.parameters.filter(_.isPathVariable).map { param =>
      s"""{
         |  "name": "${param.name}",
         |  "in": "path",
         |  "required": true,
         |  "schema": {
         |    "type": "${mapTypeToOpenApi(param.typeName)}"
         |  }
         |}""".stripMargin
    }

    s"""  "${mapping.httpMethod.toLowerCase}": {
       |    "tags": ["${mapping.controllerSimpleName}"],
       |    "summary": "${mapping.methodName}",
       |    "operationId": "${mapping.methodName}",
       |    ${if (parameters.nonEmpty) s""""parameters": [${parameters.mkString(",")}],""" else ""}
       |    "responses": {
       |      "200": {
       |        "description": "OK"
       |      }
       |    }
       |  }""".stripMargin
  }

  private def mapTypeToOpenApi(typeName: String): String = {
    typeName match {
      case t if t.endsWith("String") => "string"
      case t if t.endsWith("Integer") || t.endsWith("Int") => "integer"
      case t if t.endsWith("Long") => "integer"
      case t if t.endsWith("Boolean") => "boolean"
      case t if t.endsWith("Double") || t.endsWith("Float") => "number"
      case _ => "string"
    }
  }

  // 数据模型
  case class ControllerMapping(
                                controllerFullName: String,
                                controllerSimpleName: String,
                                methodName: String,
                                httpMethod: String,
                                path: String,
                                consumesMediaTypes: List[String],
                                producesMediaTypes: List[String],
                                parameters: List[MethodParameter]
                              )

  case class MethodParameter(
                              name: String,
                              typeName: String,
                              isPathVariable: Boolean,
                              isRequestParam: Boolean,
                              isRequestBody: Boolean
                            )

  private def matchesShiroPattern(path: String, shiroPatterns: List[String]): Boolean = {
    shiroPatterns.exists { pattern =>
      // 将/**转换为匹配任意路径段的正则表达式
      val regex = pattern
        .replace("/**", "(/[^/]+)*") // 处理 /** 模式，匹配任意数量的路径段
        .replace("/*", "/[^/]+") // 处理 /* 模式，匹配单个路径段
        .replace(".", "\\.") // 转义点号
        .replace("?", "\\?") // 转义问号

      // 确保路径以/开头
      val normalizedPath = if (path.startsWith("/")) path else "/" + path
      normalizedPath.matches(s"^$regex$$")
    }
  }

  /**
   * 从配置文件中读取Shiro路径规则
   */
  private def loadShiroPatterns(): List[String] = {
    print("Loading configuration from external file: config/filter/omni_spring_mapping_whitelist.txt")
    val source = scala.io.Source.fromFile("config/filter/omni_spring_mapping_whitelist.txt")
    try {
      source.getLines()
        .map(_.trim) // 去除首尾空格
        .filter(_.nonEmpty) // 过滤空行
        .filter(!_.startsWith("#")) // 过滤注释行
        .toList

    } finally {
      source.close()
    }
  }

  /**
   * 检查所有提取的mapping路径是否匹配Shiro规则
   */
  def checkShiroPatterns(mappings: List[ControllerMapping]): List[(ControllerMapping, Boolean)] = {
    // 从配置文件加载Shiro路径规则
    val shiroPatterns = loadShiroPatterns()

    mappings.map { mapping =>
      val normalizedPath = if (mapping.path.startsWith("/")) mapping.path else "/" + mapping.path
      (mapping, matchesShiroPattern(normalizedPath, shiroPatterns))
    }
  }

  /**
   * 打印匹配结果
   * @param matchResults 匹配结果列表
   */
  def printMatchResults(matchResults: List[(ControllerMapping, Boolean)]): Unit = {
    println("\n=== Shiro路径规则匹配结果 ===")
    matchResults.foreach { case (mapping, isMatched) =>
      val status = if (isMatched){

        println(s"Controller: ${mapping.controllerFullName}")
        println(s"Method: ${mapping.methodName}")
        println(s"Path: ${mapping.path} -> 匹配")
        println("---")
      }
    }

    // 打印统计信息
    val matchedCount = matchResults.count(_._2)
    val totalCount = matchResults.size
    println(s"\n匹配统计: 总共 $totalCount 个路径, 其中 $matchedCount 个匹配Shiro规则")
  }

  /**
   * 提取并检查所有mapping路径，返回匹配到规则的Method列表
   */
  def extractAndCheckMappings(cpg: Cpg): List[Method] = {
    val mappings = extractAllMappings(cpg)

    val matchResults = checkShiroPatterns(mappings)
    printMatchResults(matchResults)
    // 收集所有匹配到规则的Method
    matchResults
      .filter(_._2) // 只保留匹配的路径
      .map(_._1) // 获取ControllerMapping
      .flatMap { mapping =>
        // 根据controllerFullName和methodName查找对应的Method
        cpg.method
          .where(_.typeDecl.fullName(mapping.controllerFullName))
          .name(mapping.methodName)
          .l
      }
      .distinct // 去重
  }
  /**
   * 提取所有Controller的Mapping路径并保存到文件中
   * 
   * @param cpg CPG图
   * @param saveFile 保存路径的文件路径
   */
  def extractAndCheckMappingsSave(cpg: Cpg, saveFile: String): Unit = {
    val mappings = extractAllMappings(cpg)
    
    // 创建文件写入器
    import java.io.{File, PrintWriter}
    val file = new File(saveFile)

    // 创建父目录（如果不存在）
    file.getParentFile.mkdirs()
    val writer = new PrintWriter(new File(saveFile))
    
    try {
      // 写入文件头
      writer.println("# Spring Controller Mappings")
      writer.println("# Generated on " + java.time.LocalDateTime.now())
      writer.println("# Format: [PATH]")
      writer.println()
      
      // 写入每个映射的路径
      mappings.foreach { mapping =>
        writer.println(mapping.path)
      }
      
    } finally {
      writer.close()
    }
  }
}
