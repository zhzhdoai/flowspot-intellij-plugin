package omni.scan.newpass

import flatgraph.DiffGraphApplier
import io.joern.dataflowengineoss.DefaultSemantics
import io.joern.dataflowengineoss.language.*
import io.joern.dataflowengineoss.queryengine.{EngineConfig, EngineContext}
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{AstNode, CfgNode, StoredNode}
import io.shiftleft.passes.CpgPass
import io.shiftleft.semanticcpg.language.*
import omni.filter.DuplicateFlow
import omni.semantic.JavaSemanticGenerator
import omni.scan.*
import omni.scan.TaintAnalysisKeys.{SANITIZER, SINK, SOURCE}
import org.json4s.*
import org.json4s.native.JsonMethods.*
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write

import scala.collection.parallel.CollectionConverters.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}
import java.util.concurrent.{ConcurrentHashMap, Executors}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * 优化版的ScanPass，使用分批处理与并行计算来处理sources和sinks
 *
 * @param cpg CPG图实例
 * @param maxCallDepth 最大调用深度
 * @param batchSize 批处理大小，默认为100
 * @param timeoutSeconds 每个批次的超时时间（秒），默认为300秒
 */
class OptimizedScanPass(
                         cpg: Cpg,
                         queries: List[Query],
                         maxCallDepth: Int,
                         batchSize: Int = 100,
                         timeoutSeconds: Int = 300,
                         scanMode: String = "full",
                         optimizationConfig: OptimizationConfig = OptimizationConfig.default,
                         callback: ProgressCallback
                       )(implicit engineContext: EngineContext) extends CpgPass(cpg) {
  private val QueryTagTaint = "taint"
  private val pathCache = new ConcurrentHashMap[(Long, Long), List[Path]]()

  /**
   * 将sources和sinks分成批次并并行处理
   *
   * @param sources 源点列表
   * @param sinks   汇点列表
   * @return 所有路径的列表
   */
  private def batchProcessFlows(sources: List[CfgNode], sinks: List[CfgNode]): List[Path] = {
    if (sources.isEmpty || sinks.isEmpty) {
      return List.empty[Path]
    }

    //    println(s"Processing ${sources.size} sources and ${sinks.size} sinks in batches of $batchSize")

    // 只对sinks分成批次，sources不进行分组
    // val sourceBatches = sources.grouped(batchSize).toList
    val sinkBatches = sinks.grouped(batchSize).toList

    //    println(s"Processing with ${sources.size} sources and ${sinkBatches.size} sink batches")

    // 创建一个自定义的ExecutionContext，限制线程数量以避免资源耗尽
    val threadPoolSize = Math.min(Runtime.getRuntime.availableProcessors(), 20)
    val executorService = Executors.newFixedThreadPool(threadPoolSize)
    val executionContext = ExecutionContext.fromExecutor(executorService)

    try {
      // 为每个sink批次创建Future，sources不分批
      val futures = for {
        (sinkBatch, sinkIdx) <- sinkBatches.zipWithIndex
      } yield {
        Future {
          callback.updateMessage(s"Processing batch: all sources, sink $sinkIdx")
          val startTime = System.currentTimeMillis()

          // 执行数据流分析
          val paths = Try {
            sinkBatch.reachableByFlows(sources)(engineContext).l
          } match {
            case Success(result) => result
            case Failure(e) =>
              callback.updateMessage(s"Error in batch sink $sinkIdx: ${e.getMessage}")
              List.empty[Path]
          }

          val duration = (System.currentTimeMillis() - startTime) / 1000
          callback.updateMessage(s"Batch sink $sinkIdx completed in ${duration}s, found ${paths.size} paths")

          // 调用回调函数更新批次进度

          paths
        }(executionContext)
      }

      // 等待所有Future完成，不设置超时时间
      val allFutures = Future.sequence(futures)
      val result = Await.result(allFutures, Duration.Inf)

      // 合并所有批次的结果
      val allPaths = result.flatten

      // 去重并返回结果
      allPaths
    } finally {
      executorService.shutdown()
    }
  }

  /**
   * 计算路径ID
   *
   * @param path 数据流路径
   * @return 路径ID字符串
   */
  private def calculatePathId(path: Path): String = {
    path.elements.map(_.id()).mkString("-")
  }

  /**
   * 移除子路径，保留更完整的路径
   *
   * @param paths 路径列表
   * @return 去除子路径后的路径列表
   */
  private def removeSubPaths(paths: List[Path]): List[Path] = {
    if (paths.isEmpty) return paths
    
    val pathIdMap = paths.map(p => calculatePathId(p) -> p).toMap
    val filteredPathIds = DuplicateFlow.pathIdsPerSourceIdAfterDedup(pathIdMap.keySet)
    filteredPathIds.map(pathIdMap(_)).toList
  }

  /**
   * 基于sink位置的精确去重
   *
   * @param paths 路径列表
   * @return 去重后的路径列表
   */
  private def deduplicateBySinkLocation(paths: List[Path]): List[Path] = {
    if (paths.isEmpty) return paths
    
    val locationMap = scala.collection.mutable.HashMap[String, Path]()
    
    paths.foreach { path =>
      val sinkNode = path.elements.last.asInstanceOf[CfgNode]
      val sinkId = getSinkId(sinkNode)
      val locationKey = s"${sinkNode.lineNumber.getOrElse(0)}${sinkNode.file.name}${sinkId}"
      
      locationMap.get(locationKey) match {
        case Some(existingPath) if path.elements.size < existingPath.elements.size =>
          locationMap(locationKey) = path // 保留更短的路径
        case None =>
          locationMap(locationKey) = path
        case _ => // 保留现有路径
      }
    }
    
    locationMap.values.toList
  }

  /**
   * 获取sink节点的ID
   *
   * @param sinkNode sink节点
   * @return sink ID
   */
  private def getSinkId(sinkNode: CfgNode): String = {
    sinkNode.tag.nameExact(SINK).value.headOption.getOrElse("unknown")
  }

  override def run(builder: DiffGraphBuilder): Unit = {
    val (taintQueries, otherQueries) = queries.partition(_.tags.contains(QueryTagTaint))
    implicit val taggingDiffGraph: DiffGraphBuilder = builder
    implicit val finder: NodeExtensionFinder = DefaultNodeExtensionFinder
    implicit val formats = org.json4s.DefaultFormats

    taintQueries.foreach { q =>
      val queryTags = q.tags.toSet
      if (queryTags.contains(TaintAnalysisKeys.SOURCE)) {
        q.traversal(cpg).tagAsSource(q.name)

      } else if (queryTags.contains(TaintAnalysisKeys.SINK)) {
        // 调试信息：显示Sink查询的详细信息
//        println(s"[SINK] 处理Sink查询: ${q.name}")
//        println(s"[SINK] - 模式: ${q.sinkPattern}")
//        println(s"[SINK] - 分数: ${q.score}")
//        println(s"[SINK] - 类别: ${q.category}")
        
        val tagValue = write(Map(
          "name" -> q.name,
          "pattern" -> q.sinkPattern,
          "score" -> q.score.toString,
          "category" ->q.category
        ))
        
//        println(s"[SINK] 生成的tagValue: $tagValue")
//
//        // 特别关注COMMAND_INJECTION相关的Sink
//        if (q.name.contains("COMMAND_INJECTION")) {
//          println(s"[COMMAND_INJECTION] 正在处理命令注入Sink: ${q.name}")
//          println(s"[COMMAND_INJECTION] tagValue: $tagValue")
//        }
        
        q.traversal(cpg).tagAsSink(tagValue)
      } else if (queryTags.contains(TaintAnalysisKeys.SANITIZER)) {
        q.traversal(cpg).tagAsSanitizer(q.name)
      }
    }
    DiffGraphApplier.applyDiff(cpg.graph, builder)
    val queryLookup = taintQueries.map(q => q.name -> q).toMap

//    val semantics = Some(JavaSemanticGenerator.getSemantics(cpg, exportRuntimeSemantics = true))
//    val engineContext = EngineContext(DefaultSemantics().plus(semantics.getOrElse(List())), EngineConfig(maxCallDepth))
    //    SpringApiFilter.extractAndCheckMappings(cpg)
    println(
      s"Scan matched on " +
        s"${cpg.sources.size} sources, " +
        s"${cpg.sinks.size} sinks"
    )

    // 获取所有sources和sinks
    val sources = cpg.sources.l
    val sinks = cpg.sinks.l

//    println(s"Found ${sources.size} sources and ${sinks.size} sinks")
//    println(s"Scan mode: $scanMode")
//    println(s"Optimization config: ${optimizationConfig.getDescription}")


    // 创建findingGraph用于存储发现的漏洞
    val findingGraph = Cpg.newDiffGraphBuilder

    // 根据扫描模式决定是否执行数据流分析
    val paths = {
      // 完整模式：执行正常的数据流分析
      val filteredPairs = {
        // 如果数量不多，则不进行过滤
        for {
          source <- sources
          sink <- sinks
        } yield (source, sink)
      }

      callback.updateMessage(s"After filtering, processing ${filteredPairs.size} source-sink pairs")

      // 如果过滤后的对数量仍然很大，使用批处理
      val result = {
        // 重新分离sources和sinks，去重
        val filteredSources = filteredPairs.map(_._1).distinct
        val filteredSinks = filteredPairs.map(_._2).distinct
        batchProcessFlows(filteredSources, filteredSinks)
      }
      result
    }
//    val paths = sinks.reachableByFlows(sources)(engineContext).l
    // 去重并排序
    val uniquePaths = {
      // 第一步：基本去重（保留现有逻辑）
      val basicDedup = paths.sortBy(p => -p.elements.size)
        .distinctBy(p => (
          p.elements.head.asInstanceOf[CfgNode].method.fullName,
          p.elements.last.asInstanceOf[CfgNode].method.fullName
        ))
      
      // 第二步：子路径去重（可配置）
      val subPathDedup = if (optimizationConfig.enableSubPathDeduplication) {
        removeSubPaths(basicDedup)
      } else {
        println("跳过子路径去重优化")
        basicDedup
      }
      
      // 第三步：基于sink位置的精确去重（可配置）
      val locationDedup = if (optimizationConfig.enableSinkLocationDeduplication) {
        deduplicateBySinkLocation(subPathDedup)
      } else {
        subPathDedup
      }
      
      // 第四步：应用现有过滤器（可配置）
      if (optimizationConfig.enableContextFiltering) {
        locationDedup
          .filter(DuplicateFlow.filterFlowsByContext)
          .filter(DuplicateFlow.flowNotTaintedByThis)
      } else {
        locationDedup
      }
    }


    // 根据扫描模式决定是否处理没有数据流的sink
    if (sources.isEmpty) {
      callback.updateMessage("sink-no-flow mode: analyzing all sinks and sinks without data flows")
      // Sink-only模式：分析所有sink并标记没有数据流的sink

//      val sinksWithFlowsByLocation = uniquePaths.flatMap { path =>
//        path.elements.map { node => (getLocationKey(node), node) }
//      }.toMap

      // 使用 distinctBy 基于位置去重
//      val sinksWithoutFlows = sinks
//        .distinctBy(getLocationKey) // 先基于位置去重
//        .filterNot { sink =>
//          sinksWithFlowsByLocation.contains(getLocationKey(sink))
//        }
      // 为没有数据流的sink创建findings


      // 获取有数据流的 sink 位置集合
      // 获取有数据流的 sink 位置集合
      val sinkLocationsWithFlows = uniquePaths.flatMap { path =>
        path.elements
          .filter(_.tag.nameExact(SINK).nonEmpty)
          .map(getSinkLocation)
      }.toSet

      // 过滤没有数据流的sink
      val sinksWithoutFlows = sinks
        .filter(sink => !sinkLocationsWithFlows.contains(getSinkLocation(sink)))
        .distinctBy(getSinkLocation)
      sinksWithoutFlows.foreach { sink =>
        val sinkInfo = sink.tag.nameExact(SINK).value.headOption.flatMap { tagJson =>
          try {
            Some(parse(tagJson).extract[Map[String, String]])
          } catch {
            case e: Exception =>
              callback.updateMessage(s"Error parsing sink tag JSON: ${e.getMessage}")
              None
          }
        }.getOrElse(Map.empty[String, String])

        val score = sinkInfo.get("score").flatMap(s => scala.util.Try(s.toDouble).toOption).getOrElse(5.0)
        val finding = QueryWrapper.finding(
          evidence = List(sink),
          name = sinkInfo.getOrElse("name", "No Data Flow Sink"),
          author = "osword",
          title = s"Potential False Positive Sink: ${sinkInfo.getOrElse("name", "Unknown")}",
          description = s"This sink (${sinkInfo.getOrElse("name", "Unknown")}) has been defined but has no data flow paths reaching it. This might indicate either a false positive sink definition or a potential issue in the code where this sink is not being properly reached by tainted data.",
          score = score,
          sinkPattern = sinkInfo.getOrElse("pattern", "Unknown"),
          category = sinkInfo.getOrElse("category","Unknown_category")
        )
        findingGraph.addNode(finding)
      }
      }
      // 处理每个发现的漏洞路径（仅在完整模式下）
      uniquePaths.zipWithIndex.map { case (path@Path(elements), idx) =>
          val sourceQuery = elements
            .flatMap(_.tag.nameExact(SOURCE, SANITIZER, SINK).value.headOption)
            .headOption
            .map(queryLookup.apply)
          val sinkTagName = elements
            .flatMap(_.tag.nameExact(SINK).value.headOption)
            .lastOption
          val sinkInfo = sinkTagName.flatMap { tagJson =>
            try {
              Some(parse(tagJson).extract[Map[String, String]])
            } catch {
              case e: Exception =>
                callback.updateMessage(s"Error parsing sink tag JSON: ${e.getMessage}")
                None
            }
          }.getOrElse(Map.empty[String, String])

          val evidence = elements.last
          QueryWrapper.finding(elements, sinkInfo.getOrElse("name", ""), "osword", sinkInfo.getOrElse("name", ""), sinkInfo.getOrElse("pattern", ""), sinkInfo.get("score").flatMap(s => scala.util.Try(s.toDouble).toOption).getOrElse(5.0), sinkInfo.getOrElse("pattern", ""),category = sinkInfo.getOrElse("category","Unknown_category"))
        }
        .foreach(findingGraph.addNode)


      DiffGraphApplier.applyDiff(cpg.graph, findingGraph)
    println(s"OptimizedScanPass completed: ${uniquePaths.size} findings added")
    }

    //  private def buildResult(sourceQ: Option[Query], sinkQ: Option[Query]): (String, String) = {
    //    (sourceQ, sinkQ) match {
    //      case (Some(src), Some(snk)) => s"${src.title} flows to ${snk.title}" -> s"${src.description}\n${snk.description}"
    //      case (None, Some(snk)) => s"Attacker-controlled data flows to ${snk.title}" -> snk.description
    //      case (Some(src), None) => s"${src.title} data flows to a sensitive sink" -> src.description
    //      case (None, None) =>
    //        s"Attacker-controlled data flows to a sensitive sink" ->
    //          "Attacker-controlled data defining arguments to a sensitive sink may be a cause of security vulnerabilities."
    //    }
    //  }
  }

private def getSinkLocation(node: AstNode): (String, String, Int) = {
  val methodName = node match {
    case cfgNode: CfgNode => cfgNode.method.fullName
    case _ =>
      // 对于非CfgNode，尝试通过AST遍历找到包含的方法

      node.ast.isMethod.fullName.headOption.getOrElse("unknown")
  }
  (
    methodName,
    node.file.headOption.map(_.name).getOrElse(""),
    node.lineNumber.getOrElse(0)
  )
}

/**
 * OptimizedScanPass的伴生对象，提供工厂方法
 */
object OptimizedScanPass {
  /**
   * 创建OptimizedScanPass实例
   *
   * @param cpg CPG图实例
   * @param maxCallDepth 最大调用深度
   * @param batchSize 批处理大小
   * @param timeoutSeconds 超时时间（秒）
   * @return OptimizedScanPass实例
   */
  //  def apply(
  //    cpg: Cpg,
  //    maxCallDepth: Int = 4,
  //    batchSize: Int = 100,
  //    timeoutSeconds: Int = 300
  //  )(implicit engineContext: EngineContext): OptimizedScanPass = {
  //    new OptimizedScanPass(cpg, maxCallDepth, batchSize, timeoutSeconds)
  //  }

}