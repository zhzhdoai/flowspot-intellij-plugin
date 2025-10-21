package omni.scan

/**
 * Sink 规则信息类
 * 用于描述数据汇点检测规则的详细信息
 */
class SinkRuleInfo(
  val name: String,           // 规则名称
  val displayName: String,    // 显示名称
  val description: String,    // 规则描述
  val category: String = "Sink",    // 规则类别
  val severity: String = "HIGH",    // 严重程度
  val enabled: Boolean = true       // 是否启用
) {
  
  // Java 兼容的五参数构造器（包含 priority 作为 int）
  def this(name: String, displayName: String, description: String, category: String, priority: Int) = {
    this(name, displayName, description, category, 
      priority match {
        case p if p >= 8 => "CRITICAL"
        case p if p >= 6 => "HIGH"
        case p if p >= 4 => "MEDIUM"
        case _ => "LOW"
      }, true)
  }
  
  /**
   * Java 兼容的 getter 方法
   */
  def getName: String = name
  def getDisplayName: String = displayName
  def getDescription: String = description
  def getCategory: String = category
  def getSeverity: String = severity
  def getPriority: Int = getSeverityLevel  // 添加 getPriority 方法
  def isEnabled: Boolean = enabled
  
  /**
   * 创建副本并修改启用状态
   */
  def withEnabled(enabled: Boolean): SinkRuleInfo = {
    new SinkRuleInfo(name, displayName, description, category, severity, enabled)
  }
  
  /**
   * 创建副本并修改严重程度
   */
  def withSeverity(severity: String): SinkRuleInfo = {
    new SinkRuleInfo(name, displayName, description, category, severity, enabled)
  }
  
  /**
   * 获取规则的完整标识符
   */
  def getFullIdentifier: String = s"$category.$name"
  
  /**
   * 获取数值化的严重程度（用于排序）
   */
  def getSeverityLevel: Int = severity match {
    case "CRITICAL" => 8
    case "HIGH" => 6
    case "MEDIUM" => 4
    case "LOW" => 2
    case _ => 0
  }
  
  override def toString: String = s"SinkRuleInfo($name, $displayName, $severity)"
}

/**
 * SinkRuleInfo 伴生对象，提供 Java 兼容的工厂方法
 */
object SinkRuleInfo {
  
  /**
   * Java 兼容的五参数构造器（包含 priority 作为 int）
   */
  def apply(name: String, displayName: String, description: String, category: String, priority: Int): SinkRuleInfo = {
    val severity = priority match {
      case p if p >= 8 => "CRITICAL"
      case p if p >= 6 => "HIGH"
      case p if p >= 4 => "MEDIUM"
      case _ => "LOW"
    }
    new SinkRuleInfo(name, displayName, description, category, severity)
  }
  
  /**
   * 支持 severity 参数的构造器
   */
  def apply(name: String, displayName: String, description: String, severity: String): SinkRuleInfo = {
    new SinkRuleInfo(name, displayName, description, "Sink", severity)
  }
  
  /**
   * 完整参数的构造器
   */
  def apply(name: String, displayName: String, description: String, category: String, severity: String, enabled: Boolean): SinkRuleInfo = {
    new SinkRuleInfo(name, displayName, description, category, severity, enabled)
  }
}
