package omni.scan

/**
 * 扫描模式信息类
 * 用于描述不同扫描模式的配置信息
 */
class ScanModeInfo(
  val name: String,              // 模式名称
  val displayName: String,       // 显示名称
  val description: String,       // 模式描述
  val maxCallDepth: Int = 4,     // 最大调用深度
  val timeout: Int = 300,        // 超时时间（秒）
  val maxPaths: Int = 100,       // 最大路径数
  val enabled: Boolean = true    // 是否启用
) {
  
  // Java 兼容的三参数构造器
  def this(name: String, displayName: String, description: String) = {
    this(name, displayName, description, 4, 300, 100, true)
  }
  
  /**
   * Java 兼容的 getter 方法
   */
  def getName: String = name
  def getMode: String = name  // 添加 getMode 方法作为 getName 的别名
  def getDisplayName: String = displayName
  def getDescription: String = description
  def getMaxCallDepth: Int = maxCallDepth
  def getTimeout: Int = timeout
  def getMaxPaths: Int = maxPaths
  def isEnabled: Boolean = enabled
  
  /**
   * 创建副本并修改启用状态
   */
  def withEnabled(enabled: Boolean): ScanModeInfo = {
    new ScanModeInfo(name, displayName, description, maxCallDepth, timeout, maxPaths, enabled)
  }
  
  /**
   * 创建副本并修改调用深度
   */
  def withMaxCallDepth(depth: Int): ScanModeInfo = {
    new ScanModeInfo(name, displayName, description, depth, timeout, maxPaths, enabled)
  }
  
  /**
   * 创建副本并修改超时时间
   */
  def withTimeout(timeoutSeconds: Int): ScanModeInfo = {
    new ScanModeInfo(name, displayName, description, maxCallDepth, timeoutSeconds, maxPaths, enabled)
  }
  
  /**
   * 创建副本并修改最大路径数
   */
  def withMaxPaths(paths: Int): ScanModeInfo = {
    new ScanModeInfo(name, displayName, description, maxCallDepth, timeout, paths, enabled)
  }
  
  /**
   * 检查配置是否有效
   */
  def isValid: Boolean = {
    maxCallDepth > 0 && timeout > 0 && maxPaths > 0
  }
  
  /**
   * 获取性能等级（基于配置参数）
   */
  def getPerformanceLevel: String = {
    val score = maxCallDepth + (timeout / 60) + (maxPaths / 50)
    score match {
      case s if s <= 5 => "FAST"
      case s if s <= 10 => "BALANCED"
      case s if s <= 20 => "THOROUGH"
      case _ => "INTENSIVE"
    }
  }
  
  override def toString: String = s"ScanModeInfo($name, $displayName, depth=$maxCallDepth, timeout=${timeout}s)"
}

/**
 * 预定义的扫描模式
 */
object ScanModeInfo {
  
  /**
   * 快速扫描模式
   */
  val FAST: ScanModeInfo = new ScanModeInfo(
    name = "fast",
    displayName = "快速扫描",
    description = "快速扫描模式，适用于开发阶段的快速检查",
    maxCallDepth = 2,
    timeout = 120,
    maxPaths = 50
  )
  
  /**
   * 平衡扫描模式
   */
  val BALANCED: ScanModeInfo = new ScanModeInfo(
    name = "balanced",
    displayName = "平衡扫描",
    description = "平衡扫描模式，在速度和准确性之间取得平衡",
    maxCallDepth = 4,
    timeout = 300,
    maxPaths = 100
  )
  
  /**
   * 深度扫描模式
   */
  val THOROUGH: ScanModeInfo = new ScanModeInfo(
    name = "thorough",
    displayName = "深度扫描",
    description = "深度扫描模式，提供更全面的漏洞检测",
    maxCallDepth = 6,
    timeout = 600,
    maxPaths = 200
  )
  
  /**
   * 获取所有预定义的扫描模式
   */
  def getAllModes: List[ScanModeInfo] = List(FAST, BALANCED, THOROUGH)
  
  /**
   * 根据名称获取扫描模式
   */
  def getByName(name: String): Option[ScanModeInfo] = {
    getAllModes.find(_.name.equalsIgnoreCase(name))
  }

  /**
   * Java 兼容的三参数构造器
   */
  def apply(name: String, displayName: String, description: String): ScanModeInfo = {
    new ScanModeInfo(name, displayName, description)
  }
  
  /**
   * 完整参数的构造器
   */
  def apply(name: String, displayName: String, description: String, maxCallDepth: Int, timeout: Int, maxPaths: Int): ScanModeInfo = {
    new ScanModeInfo(name, displayName, description, maxCallDepth, timeout, maxPaths)
  }
  
  /**
   * 全参数的构造器
   */
  def apply(name: String, displayName: String, description: String, maxCallDepth: Int, timeout: Int, maxPaths: Int, enabled: Boolean): ScanModeInfo = {
    new ScanModeInfo(name, displayName, description, maxCallDepth, timeout, maxPaths, enabled)
  }
}
