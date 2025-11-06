package omni.scan

import omni.logging.FlowSpotLog
import omni.util.{FlowSpotLogger, OutputRedirector, ProgressCallbackFactory, Logging}
import io.shiftleft.codepropertygraph.generated.nodes.{CfgNode, Identifier, Member, MethodParameterIn}
import io.joern.dataflowengineoss.language.*
import io.joern.dataflowengineoss.DefaultSemantics
import io.joern.dataflowengineoss.layers.dataflows.{OssDataFlow, OssDataFlowOptions}
import io.joern.dataflowengineoss.layers.dataflows.{OssDataFlow, OssDataFlowOptions}
import io.joern.dataflowengineoss.queryengine.{EngineConfig, EngineContext}
import io.joern.x2cpg.X2Cpg
import io.shiftleft.SerializedCpg
import io.shiftleft.passes.CpgPassBase
import io.shiftleft.semanticcpg.layers.LayerCreatorContext
import io.shiftleft.semanticcpg.language.*
import io.shiftleft.codepropertygraph.cpgloading.CpgLoader
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes}
import omni.scan.generate.sources.{HttpRequestTagger, NettyHandlerTagger, RabbitMqSourceTagger, RequestBodyTagger, SpringMappingTagger, Struts2Tagger, ThriftHandlerTagger, WebServletTagger, WebSocketTagger}
import better.files.File

import java.util.ArrayList
import java.nio.file.{Files, Paths, StandardCopyOption}
import omni.flowspot.project.FlowSpotProjectStats
import omni.flowspot.core.FlowSpotBugCollection
import omni.flowspot.project.FlowSpotProject
import omni.flowspot.annotations.{FlowSpotEnhancedSourceLineAnnotation, FlowSpotSourceLineAnnotation}
import omni.flowspot.core.FlowSpotBugInstance
import omni.filter.DuplicateFlow
import omni.scan.ScannerFindingExtension
import omni.test.SpringApiFilter
import omni.scan.newpass.{BeanValidationDataFlowPass, OptimizedScanPass}

import java.nio.file.Paths
import scala.jdk.CollectionConverters.*

/**
 * TestScan类负责执行漏洞分析流程，包括CPG生成、数据流分析和漏洞检测
 */
// 定义进度回调接口
trait ProgressCallback {
  def updateMessage(message: String): Unit
  def updateProgress(progress: Int): Unit
}

object FlowSpot {

  /**
   * 获取rt.jar资源文件路径，提取到临时文件
   */
  private def getRtJarPath: Option[String] = {
    try {
      val inputStream = getClass.getResourceAsStream("/rt.jar")
      if (inputStream != null) {
        // 创建临时文件
        val tempFile = java.io.File.createTempFile("flowspot-rt", ".jar")
        tempFile.deleteOnExit()
        
        // 复制资源到临时文件
        val outputStream = new java.io.FileOutputStream(tempFile)
        try {
          val buffer = new Array[Byte](8192)
          var bytesRead = inputStream.read(buffer)
          while (bytesRead != -1) {
            outputStream.write(buffer, 0, bytesRead)
            bytesRead = inputStream.read(buffer)
          }
          
          val tempPath = tempFile.getAbsolutePath
          FlowSpotLogger.info(s"Extracted rt.jar to temporary file: $tempPath", Some("FlowSpot"))
          Some(tempPath)
        } finally {
          inputStream.close()
          outputStream.close()
        }
      } else {
        FlowSpotLogger.warn("rt.jar resource not found in classpath", Some("FlowSpot"))
        None
      }
    } catch {
      case e: Exception =>
        FlowSpotLogger.error(s"Failed to extract rt.jar: ${e.getMessage}", Some("FlowSpot"), Some(e))
        None
    }
  }

  private def getSelectedSinkRulesFromProject(config: FlowSpotProjectConfig,callback: ProgressCallback): Set[String] = {
    try {
      // 从 FlowSpotProjectConfig 对象中获取选中的 sink 规则
      import scala.jdk.CollectionConverters._
      val javaSet = config.getSelectedSinkRules
      if (javaSet != null) {
        val result = javaSet.asScala.toSet
        callback.updateMessage(s"从配置中获取到的Sink规则: ${result.mkString(", ")}")
        result
      } else {
        callback.updateMessage("配置中的Sink规则为null")
        Set.empty[String]
      }
    } catch {
      case e: Exception =>
        callback.updateMessage(s"从 FlowSpotProjectConfig 对象获取选中的 sink 规则时出错: ${e.getMessage}")
        e.printStackTrace()
        Set.empty[String]
    }
  }

  /**
   * 从 FlowSpotProjectConfig 对象获取用户选择的 source 规则
   */
  private def getSelectedSourceRulesFromProject(config: FlowSpotProjectConfig,callback:ProgressCallback): Set[String] = {
    try {
      // 从 FlowSpotProjectConfig 对象中获取选中的 source 规则
      import scala.jdk.CollectionConverters._
      val javaSet = config.getSelectedSourceRules
      if (javaSet != null) {
        javaSet.asScala.toSet
      } else {
        Set.empty[String]
      }
    } catch {
      case e: Exception =>
        callback.updateMessage(s"从 FlowSpotProjectConfig 对象获取选中的 source 规则时出错: ${e.getMessage}")
        e.printStackTrace()
        Set.empty[String]
    }
  }



  

  /**
   * 辅助方法，用于运行CPG pass
   */
  protected def runPass(pass: CpgPassBase, context: LayerCreatorContext, index: Int = 0): Unit = {
    FlowSpotLog.info(s"Running pass: ${pass.getClass.getName}")
    pass.createAndApply()
    FlowSpotLog.info(s"Pass completed: ${pass.getClass.getName}")
    if (index % 10 == 0) {
      FlowSpotLog.debug("Collecting garbage...")
      System.gc()
    }
  }

  protected def initSerializedCpg(outputDir: Option[String], passName: String, index: Int = 0): SerializedCpg = {
    outputDir match {
      case Some(dir) => new SerializedCpg((File(dir) / s"${index}_$passName").path.toAbsolutePath.toString)
      case None => new SerializedCpg()
    }
  }

  /**
   * 处理漏洞路径，创建FindBugs/SpotBugs格式的漏洞实例
   *
   * @param path         漏洞路径
   * @param bugType      漏洞类型
   * @param priority     优先级
   * @param pattern      匹配的模式
   * @param bugInstances 漏洞实例集合
   */
  def processBugPath(path: Path,category:String, bugType: String, priority: Int, pattern: String, bugInstances: ArrayList[FlowSpotBugInstance]): Unit = {
    val bugInstance = new FlowSpotBugInstance(category, bugType, priority)
    path.elements.foreach { astNode =>
      val nodeType = astNode.getClass.getSimpleName
      val lineNumber = astNode.lineNumber.getOrElse("N/A").toString
      val fileName = astNode.file.name.headOption.getOrElse("N/A")
      val columnNumberStart = astNode.columnNumber.getOrElse(-1)
      val columnNumberEnd = columnNumberStart + astNode.code.length - 1
      astNode match {
        case member: Member =>
          val tracked = member.name
          val methodName = "<not-in-method>"
          Seq(nodeType, tracked, lineNumber, methodName, fileName)
        case cfgNode: CfgNode =>
          val method = cfgNode.method
          val className = method.typeDecl.fullName.headOption.getOrElse("Unknown")
          val methodName = method.name
          val statement = cfgNode match {
            case _: MethodParameterIn =>
              val paramsPretty = method.parameter.toList.sortBy(_.index).map(_.code).mkString(", ")
              s"$methodName($paramsPretty)"
            case _ => cfgNode.statement.repr
          }
          val flowSpotSourceLineAnnotation = new FlowSpotEnhancedSourceLineAnnotation(className,fileName,lineNumber.toInt,lineNumber.toInt,columnNumberStart,columnNumberEnd);
          flowSpotSourceLineAnnotation.setCode(statement);
          flowSpotSourceLineAnnotation.setIdentifierName(cfgNode match {
            case _: Identifier => cfgNode.asInstanceOf[Identifier].name
            case _ => cfgNode.statement.repr
          })
          flowSpotSourceLineAnnotation.setNodeType(nodeType);
          flowSpotSourceLineAnnotation.setMethodName(methodName);
          bugInstance.add(flowSpotSourceLineAnnotation)
      }
    }
    bugInstances.add(bugInstance)
  }






  // 重载方法：接受 FlowSpotProjectConfig 参数
  def doAnalysis(config: FlowSpotProjectConfig, callback: ProgressCallback): FlowSpotBugCollection = {
    doAnalysisWithConfig(config.getFlowSpotProject, config, callback)
  }

  // 重载方法：接受 FlowSpotProjectConfig 参数，使用默认回调
  def doAnalysis(config: FlowSpotProjectConfig): FlowSpotBugCollection = {
    val defaultCallback = new ProgressCallback {
      override def updateMessage(message: String): Unit = FlowSpotLog.progress(message)
      override def updateProgress(progress: Int): Unit = FlowSpotLog.progress(s"Progress: $progress%")
    }
    doAnalysisWithConfig(config.getFlowSpotProject, config, defaultCallback)
  }

  // CLI版本的doAnalysis方法，使用自定义回调接口
  def doAnalysis(project: FlowSpotProject, callback: ProgressCallback): FlowSpotBugCollection = {
    // 创建 FlowSpotProjectConfig
    val config = new FlowSpotProjectConfig(project)
    doAnalysisWithConfig(project, config, callback)
  }

  // CLI版本的doAnalysis方法，使用默认回调
  def doAnalysis(project: FlowSpotProject): FlowSpotBugCollection = {
    val defaultCallback = new ProgressCallback {
      override def updateMessage(message: String): Unit = FlowSpotLog.progress(message)
      override def updateProgress(progress: Int): Unit = FlowSpotLog.progress(s"Progress: $progress%")
    }
    doAnalysisWithConfig(project, new FlowSpotProjectConfig(project), defaultCallback)
  }

  def main(args: Array[String]): Unit = {
    val analysisTargetPath = "xxx"
    val cpgPath = s"$analysisTargetPath/cpg.bin"
    val baseArgs = Array(analysisTargetPath, "--output", cpgPath, "--delombok-mode", "no-delombok")
    io.joern.javasrc2cpg.Main.main(baseArgs)
    val tempDir = Files.createTempDirectory("flowspot_cpg_")
    val tempCpgPath = tempDir.resolve("cpg.bin")
    Files.copy(Paths.get(cpgPath), tempCpgPath, StandardCopyOption.REPLACE_EXISTING)
    val cpg = CpgLoader.load(tempCpgPath.toString)
    val context = new LayerCreatorContext(cpg)
    X2Cpg.applyDefaultOverlays(cpg)
    new OssDataFlow(new OssDataFlowOptions(semantics = DefaultSemantics())).run(context)
    implicit val engineContext: EngineContext = EngineContext(config = EngineConfig(maxCallDepth = 4))
    println(cpg.call.methodFullName(".*java\\.lang\\.ProcessBuilder\\.start.*").argument.reachableByFlows(cpg.method.where(_.annotation.name(".*Mapping")).parameter)(engineContext).filter(DuplicateFlow.filterFlowsByContext).filter(DuplicateFlow.flowNotTaintedByThis).p)

  }
  // 内部实现方法 - 重构为使用 FlowSpotBugCollection
  private def doAnalysisWithConfig(flowSpotProject: FlowSpotProject, config: FlowSpotProjectConfig, callback: ProgressCallback): FlowSpotBugCollection = {
    // 初始化日志系统
    val actualProjectBasePath = config.getBaseProjectPath
    FlowSpotLogger.initialize(actualProjectBasePath)
    
    // 启动输出重定向（拦截println输出）
    OutputRedirector.startRedirection()
    
    // 创建带日志记录的进度回调
    val loggingCallback = ProgressCallbackFactory.createLoggingCallback(callback)
    
    val bugCollection = new FlowSpotBugCollection(flowSpotProject.getProjectName)
    val projectStats = new FlowSpotProjectStats(flowSpotProject.getProjectName)

    // 使用updateStatus方法同时更新状态标签和消息区域
    def updateStatus(message: String): Unit = {
      FlowSpotLogger.info(message, Some("FlowSpot"))
      loggingCallback.updateMessage(message)
    }
    
    // 更新进度的方法
    def updateProgress(progress: Int): Unit = {
      FlowSpotLogger.logProgress(progress, "Analysis Progress")
      loggingCallback.updateProgress(progress)
    }

    // 记录分析开始（添加分隔符）
    val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    FlowSpotLogger.info("\n" + "="*80, Some("FlowSpot"))
    FlowSpotLogger.info(s"=== FlowSpot 分析开始 - $timestamp ===", Some("FlowSpot"))
    FlowSpotLogger.info("="*80, Some("FlowSpot"))
//    FlowSpotLogger.getCurrentLogFile.foreach { logFile =>
//      FlowSpotLogger.info(s"日志文件: ${logFile.toAbsolutePath}", Some("FlowSpot"))
//      updateStatus(s"使用日志文件: ${logFile.getFileName}")
//    }

    // 使用 FlowSpotProjectConfig 获取路径信息
    val analysisTargetPath = config.getAnalysisTargetPath

    FlowSpotLogger.info(s"分析目标路径: $analysisTargetPath", Some("FlowSpot"))
    FlowSpotLogger.info(s"项目根目录路径: $actualProjectBasePath", Some("FlowSpot"))
    updateProgress(5) // 初始化完成 5%

    // 如果需要反编译，这里可以添加反编译代码，类似于Omni.doAnalysis
    val enableDecompile = config.isDecompileEnabled
    if (enableDecompile) {
      updateStatus("启用反编译模式")
      // 反编译逻辑
    }

    // 生成CPG（在分析目标路径下）
    val cpgPath = s"$analysisTargetPath/cpg.bin"
    println(s"生成CPG: $cpgPath")
    updateProgress(10) // CPG初始化 10%

    val cpgFile = File(cpgPath)

    if (!cpgFile.exists) {
      // 构建参数数组，包含rt.jar路径（如果可用）
      val baseArgs = Array(analysisTargetPath, "--output", cpgPath, "--delombok-mode", "no-delombok")
      val args = getRtJarPath match {
        case Some(rtJarPath) =>
          FlowSpotLogger.info(s"Using rt.jar from: $rtJarPath", Some("FlowSpot"))
          baseArgs ++ Array("--jdk-path", rtJarPath)
        case None =>
          FlowSpotLogger.warn("rt.jar not found, proceeding without JDK path", Some("FlowSpot"))
          baseArgs
      }

        try {
          FlowSpotLogger.info("开始生成CPG......")
          io.joern.javasrc2cpg.Main.main(args)
          FlowSpotLogger.info("CPG生成完成")
          updateProgress(30) // CPG生成完成 30%
        } catch {
          case e: Exception =>
            FlowSpotLogger.info(s"CPG生成失败: ${e.getMessage}")
            throw e
        }

    } else {
      FlowSpotLogger.info("发现已存在的CPG文件，跳过生成步骤")
      updateProgress(30) // 跳过CPG生成 30%
    }

    // 复制CPG文件到临时目录并加载
    FlowSpotLogger.info("复制CPG文件到临时目录...")
    updateProgress(35) // 开始复制CPG 35%

    // 创建临时目录
    val tempDir = Files.createTempDirectory("flowspot_cpg_")
    val tempCpgPath = tempDir.resolve("cpg.bin")

    // 复制CPG文件到临时目录
    Files.copy(Paths.get(cpgPath), tempCpgPath, StandardCopyOption.REPLACE_EXISTING)
    FlowSpotLogger.info(s"CPG文件已复制到临时目录: ${tempCpgPath}")

    FlowSpotLogger.info("加载CPG文件...")
    updateProgress(40) // 开始加载CPG 40%


    val cpg = CpgLoader.load(tempCpgPath.toString)
    updateProgress(50) // CPG加载完成 50%
    val context = new LayerCreatorContext(cpg)
    FlowSpotLogger.info("应用默认覆盖层...")
    X2Cpg.applyDefaultOverlays(cpg)
    updateProgress(55) // 覆盖层应用完成 55%
    FlowSpotLogger.info("运行数据流分析...")
    new OssDataFlow(new OssDataFlowOptions(semantics = DefaultSemantics())).run(context)
    implicit val engineContext: EngineContext = EngineContext(config = EngineConfig(maxCallDepth = 4))
//    val flowSemantics = JavaSemanticGenerator.getSemantics(cpg, exportRuntimeSemantics = true)
//    new OssDataFlow(new OssDataFlowOptions(semantics = DefaultSemantics().plus(Some(flowSemantics).getOrElse(List())))).create(context)
    updateProgress(60) // 数据流分析完成 60%

    FlowSpotLogger.info("执行漏洞扫描...")

    // 初始化漏洞分析器
    val analyzer = new Analyzer()

    // 构建项目根目录的sinks.json路径（统一配置管理）
    val projectSinksJsonPath = s"$actualProjectBasePath/.flowspot/config/sinks.json"
    val sinksJsonFile = new java.io.File(projectSinksJsonPath)

    FlowSpotLogger.info(s"使用配置文件: $projectSinksJsonPath")
    
    // 获取所有sink规则（使用项目根目录的sinks.json）
    val sinkQueries = analyzer.getSinksQuery(projectSinksJsonPath)
    updateProgress(65) // 规则加载完成 65%
    
    // 获取所有source规则
    val sourceQueries = analyzer.getSourcesQuery()
    updateProgress(70) // 所有规则加载完成 70%

    // 从 FlowSpotProjectConfig 对象中获取用户选择的规则
    val selectedSinkRules = getSelectedSinkRulesFromProject(config,callback)
    val selectedSourceRules = getSelectedSourceRulesFromProject(config,callback)

    updateProgress(72) // 规则统计完成 72%
    
    if (selectedSinkRules.nonEmpty) {
      FlowSpotLogger.info(s"选中的Sink规则: ${selectedSinkRules.mkString(", ")}")
    }
    if (selectedSourceRules.nonEmpty) {
      FlowSpotLogger.info(s"选中的Source规则: ${selectedSourceRules.mkString(", ")}")
    }

    // 根据用户选择筛选sink规则
    val filteredSinkQueries = if (selectedSinkRules.isEmpty) {
      // 如果用户没有选择任何规则，使用所有规则
      FlowSpotLogger.info("未选择任何Sink规则，使用所有规则")
      sinkQueries
    } else {
      // 筛选用户选择的sink规则
      FlowSpotLogger.info("开始筛选Sink规则...")
      FlowSpotLogger.info(s"总共有 ${sinkQueries.size} 个Sink查询")
      FlowSpotLogger.info(s"选中的Sink规则: ${selectedSinkRules.mkString(", ")}")
      
      val filteredSinks = sinkQueries.filter { query =>
        // 移除query.name中的数字后缀，然后检查是否在选中规则中
        val baseQueryName = query.name.replaceAll("_\\d+$", "")
        val isSelected = selectedSinkRules.contains(baseQueryName)
        isSelected
      }
      FlowSpotLogger.info(s"筛选后的Sink查询数量: ${filteredSinks.size}")
      
      // 显示筛选后的查询名称
      
      filteredSinks
    }
    updateProgress(75) // Sink规则筛选完成 75%

    // 根据用户选择筛选source规则
    val filteredSourceQueries = sourceQueries.filter { query =>
      selectedSourceRules.contains(query.name.replaceAll("_\\d+$", ""))
    }
    updateProgress(78) // Source规则筛选完成 78%

    // 合并所有查询
    val queriesAfterFilter = filteredSourceQueries ++ filteredSinkQueries

    updateStatus(s"总计查询: ${queriesAfterFilter.size}, 包括 ${filteredSinkQueries.size} 个Sink查询, ${filteredSourceQueries.size} 个Source查询")
    updateProgress(80) // 查询合并完成 80%

    // 运行漏洞扫描
    updateStatus("初始化漏洞扫描器...")



    val scanMode = config.getScanMode
    updateProgress(82) // 扫描器初始化完成 82%

    updateStatus("开始执行漏洞扫描...")
    
    // 获取优化配置（如果配置中有的话，否则使用默认配置）
    val optimizationConfig = Option(config.getOptimizationConfig).getOrElse(OptimizationConfig.default)
    updateStatus(s"优化配置: ${optimizationConfig.getDescription}")
    
    new OptimizedScanPass(cpg, queriesAfterFilter, 4, 100, 300, scanMode, optimizationConfig, callback)(engineContext).run(Cpg.newDiffGraphBuilder)
    updateProgress(90) // 漏洞扫描完成 90%


    updateStatus("运行Bean验证数据流分析...")
    new BeanValidationDataFlowPass(cpg).run(Cpg.newDiffGraphBuilder)
    updateProgress(92) // Bean验证分析完成 92%

    updateStatus("运行跨线程分析...")
//    new crossThreadPass(cpg).run(Cpg.newDiffGraphBuilder)
    updateProgress(95) // 跨线程分析完成 95%

    updateStatus("漏洞扫描完成，开始处理漏洞结果...")
    updateStatus(s"漏洞数: ${cpg.finding.size}")
    updateProgress(97) // 漏洞结果统计完成 97%

    // 保存扫描结果
//    ScanResultManager.saveResults(projectName = flowSpotProject.getProjectName, projectPath = projectPath, cpg.finding)

    // 处理漏洞结果并转换为FlowSpotBugInstance
    updateStatus("开始处理漏洞结果...")
    val bugInstances = new ArrayList[FlowSpotBugInstance]()
    cpg.finding.foreach { finding =>
      // 安全地获取evidence并转换为List
      val evidence = finding.evidence.toList.asInstanceOf[List[io.shiftleft.codepropertygraph.generated.nodes.AstNode]]
      processBugPath(
        path = Path(evidence),
        category = finding.category,
        bugType = finding.name,
        priority = finding.score.toInt,
        pattern = finding.sinkPattern,
        bugInstances = bugInstances
      )
    }
    updateProgress(98) // 漏洞结果处理完成 98%

    // 直接添加 FlowSpotBugInstance 到 FlowSpotBugCollection
    import scala.jdk.CollectionConverters._
    bugInstances.asScala.foreach(bugInstance => 
      bugCollection.add(bugInstance)
    )

    // 保存扫描结果到数据库
    updateStatus("保存扫描结果...")
//    ScanResultManager.saveResults(projectName = flowSpotProject.getProjectName, projectPath = projectPath, cpg.finding)

    // 提取Spring API映射（如果适用，保存到项目根目录）
    updateStatus("提取Spring API映射...")
    SpringApiFilter.extractAndCheckMappingsSave(cpg, actualProjectBasePath + "/.flowspot/" + "spring_url_mapping.txt")

    updateStatus("漏洞分析完成")
    updateProgress(100) // 分析完全完成 100%
    
    // 记录分析结束
    val endTimestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    FlowSpotLogger.info("="*80, Some("FlowSpot"))
    FlowSpotLogger.info(s"=== FlowSpot 分析结束 - $endTimestamp ===", Some("FlowSpot"))
    FlowSpotLogger.info(s"找到 ${bugCollection.size()} 个漏洞", Some("FlowSpot"))
    FlowSpotLogger.info("="*80 + "\n", Some("FlowSpot"))
    
    // 清理旧日志文件（保留最近7天）
    FlowSpotLogger.cleanupOldLogs(7)
    
    // 停止输出重定向
    OutputRedirector.stopRedirection()
    
    // 关闭日志系统
    FlowSpotLogger.shutdown()
    
    cpg.close()

    bugCollection
  }

}