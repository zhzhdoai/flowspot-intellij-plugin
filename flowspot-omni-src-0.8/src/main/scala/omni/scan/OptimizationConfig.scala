package omni.scan

/**
 * 扫描优化配置类
 * 控制数据流分析中的各种去重和过滤选项
 */
case class OptimizationConfig(
  // 子路径去重：移除被其他路径包含的子路径，保留更完整的数据流路径
  enableSubPathDeduplication: Boolean = true,
  
  // Sink位置去重：基于sink的文件位置进行精确去重，避免相同位置的重复报告
  enableSinkLocationDeduplication: Boolean = true,
  
  // 上下文过滤：应用现有的上下文过滤器，移除特定上下文中的误报
  enableContextFiltering: Boolean = true
) {
  
  /**
   * 获取配置的简短描述
   */
  def getDescription: String = {
    val enabled = List(
      if (enableSubPathDeduplication) Some("子路径去重") else None,
      if (enableSinkLocationDeduplication) Some("位置去重") else None,
      if (enableContextFiltering) Some("上下文过滤") else None
    ).flatten
    
    if (enabled.nonEmpty) {
      s"已启用: ${enabled.mkString(", ")}"
    } else {
      "所有优化已禁用"
    }
  }
  
  /**
   * 获取详细的配置说明
   */
  def getDetailedDescription: String = {
    s"""扫描优化配置:
       |• 子路径去重: ${if (enableSubPathDeduplication) "启用" else "禁用"} - 移除被包含的短路径，保留完整数据流
       |• Sink位置去重: ${if (enableSinkLocationDeduplication) "启用" else "禁用"} - 基于文件位置去重，避免重复报告
       |• 上下文过滤: ${if (enableContextFiltering) "启用" else "禁用"} - 应用上下文过滤器，减少误报
       |""".stripMargin
  }
}

object OptimizationConfig {
  /**
   * 默认配置：所有优化都启用
   */
  val default: OptimizationConfig = OptimizationConfig()
  
  /**
   * 无优化配置：所有优化都禁用，用于调试或特殊需求
   */
  val noOptimization: OptimizationConfig = OptimizationConfig(
    enableSubPathDeduplication = false,
    enableSinkLocationDeduplication = false,
    enableContextFiltering = false
  )
  
  /**
   * 轻量优化配置：只启用基本的位置去重
   */
  val lightOptimization: OptimizationConfig = OptimizationConfig(
    enableSubPathDeduplication = false,
    enableSinkLocationDeduplication = true,
    enableContextFiltering = false
  )
}
