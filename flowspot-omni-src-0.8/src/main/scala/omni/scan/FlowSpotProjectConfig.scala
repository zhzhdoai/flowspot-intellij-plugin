package omni.scan

import omni.flowspot.project.FlowSpotProject

/**
 * FlowSpot 项目配置类
 * 包装 FlowSpotProject 并提供便捷的访问方法
 */
class FlowSpotProjectConfig(
  val flowSpotProject: FlowSpotProject
) {
  
  /**
   * 获取项目名称
   */
  def getProjectName: String = flowSpotProject.getProjectName
  
  /**
   * 获取项目路径（分析目标路径）
   */
  def getProjectPath: String = {
    // 优先使用配置的分析目标路径
    val analysisTargetPath = flowSpotProject.getAnalysisTargetPath
    if (analysisTargetPath != null && !analysisTargetPath.isEmpty) {
      return analysisTargetPath
    }
    
    // 后备方案：从文件列表推导
    val sourceFiles = flowSpotProject.getSourceDirList
    if (!sourceFiles.isEmpty) {
      sourceFiles.get(0)
    } else {
      val files = flowSpotProject.getFileList
      if (!files.isEmpty) {
        files.get(0)
      } else {
        ""
      }
    }
  }
  
  /**
   * 获取分析目标路径
   */
  def getAnalysisTargetPath: String = {
    Option(flowSpotProject.getAnalysisTargetPath).getOrElse(getProjectPath)
  }
  
  /**
   * 获取项目根目录路径（用于配置管理）
   */
  def getBaseProjectPath: String = {
    Option(flowSpotProject.getBaseProjectPath).getOrElse(getProjectPath)
  }
  
  /**
   * 是否启用反编译
   */
  def isDecompileEnabled: Boolean = flowSpotProject.getDecompile
  
  /**
   * 获取扫描模式
   */
  def getScanMode: String = flowSpotProject.getScanMode
  
  /**
   * 获取选中的 Source 规则
   */
  def getSelectedSourceRules: java.util.Set[String] = {
    // FlowSpotProject 已经返回 Java Set，不需要转换
    flowSpotProject.getSelectedSourceRules
  }
  
  /**
   * 获取选中的 Sink 规则
   */
  def getSelectedSinkRules: java.util.Set[String] = {
    // FlowSpotProject 已经返回 Java Set，不需要转换
    flowSpotProject.getSelectedSinkRules
  }
  
  /**
   * 获取优化配置
   */
  def getOptimizationConfig: OptimizationConfig = {
    // 如果 FlowSpotProject 有优化配置，使用它；否则使用默认配置
    Option(flowSpotProject.getOptimizationConfig).getOrElse(OptimizationConfig.default)
  }
  
  /**
   * 获取底层的 SpotBugs Project 对象
   */
  
  /**
   * 获取 FlowSpotProject 对象
   */
  def getFlowSpotProject: FlowSpotProject = flowSpotProject
  
  override def toString: String = {
    s"FlowSpotProjectConfig(name=${getProjectName}, path=${getProjectPath}, " +
    s"decompile=${isDecompileEnabled}, mode=${getScanMode}, " +
    s"sourceRules=${getSelectedSourceRules.size()}, sinkRules=${getSelectedSinkRules.size()})"
  }
}

/**
 * FlowSpotProjectConfig 伴生对象
 */
object FlowSpotProjectConfig {
  
  /**
   * 创建 FlowSpot 项目配置
   */
  def create(projectPath: String,
            projectName: String,
            enableDecompile: Boolean = false,
            scanMode: String = "balanced",
            selectedSourceRules: java.util.Set[String] = new java.util.HashSet[String](),
            selectedSinkRules: java.util.Set[String] = new java.util.HashSet[String]()): FlowSpotProjectConfig = {
    
    val flowSpotProject = new FlowSpotProject()
    flowSpotProject.setProjectName(projectName)
    flowSpotProject.setDecompile(enableDecompile)
    flowSpotProject.setScanMode(scanMode)
    
    // 转换 Java Set 到 Scala Set 再到 Java Set
    import scala.jdk.CollectionConverters._
    flowSpotProject.setSelectedSourceRules(selectedSourceRules.asScala.toSet.asJava)
    flowSpotProject.setSelectedSinkRules(selectedSinkRules.asScala.toSet.asJava)
    
    // 添加项目文件/目录
    val file = new java.io.File(projectPath)
    if (file.exists()) {
      if (file.isDirectory) {
        // 对于目录，既添加到源目录列表，也添加到文件列表
        // 这样 TestScan.doAnalysis 可以通过 getFile(0) 获取到路径
        flowSpotProject.addSourceDir(projectPath)
        flowSpotProject.addFile(projectPath)  // 添加到文件列表以供 TestScan 使用
      } else {
        flowSpotProject.addFile(projectPath)
      }
    } else {
      // 即使文件不存在，也添加路径以避免空列表错误
      flowSpotProject.addFile(projectPath)
    }
    
    new FlowSpotProjectConfig(flowSpotProject)
  }
  
  /**
   * 从现有的 SpotBugs Project 对象创建配置（向后兼容）
   */
  
  /**
   * 从 FlowSpotProject 对象创建配置（推荐使用）
   */
  def fromFlowSpotProject(flowSpotProject: FlowSpotProject): FlowSpotProjectConfig = {
    new FlowSpotProjectConfig(flowSpotProject)
  }
}
