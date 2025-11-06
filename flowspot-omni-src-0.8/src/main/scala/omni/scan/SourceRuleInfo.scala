package omni.scan

/**
 * Source 规则信息类
 * 用于描述数据源检测规则的详细信息
 */
class SourceRuleInfo(
  val name: String,           // 规则名称
  val displayName: String,    // 显示名称
  val description: String,    // 规则描述
  val category: String = "Source",  // 规则类别
  val enabled: Boolean = true       // 是否启用
) {
  
  // Java 兼容的三参数构造器
  def this(name: String, displayName: String, description: String) = {
    this(name, displayName, description, "Source", true)
  }
  
  /**
   * Java 兼容的 getter 方法
   */
  def getName: String = name
  def getDisplayName: String = displayName
  def getDescription: String = description
  def getCategory: String = category
  def isEnabled: Boolean = enabled
  
  /**
   * 创建副本并修改启用状态
   */
  def withEnabled(enabled: Boolean): SourceRuleInfo = {
    new SourceRuleInfo(name, displayName, description, category, enabled)
  }
  
  /**
   * 获取规则的完整标识符
   */
  def getFullIdentifier: String = s"$category.$name"
  
  override def toString: String = s"SourceRuleInfo($name, $displayName)"
}

/**
 * SourceRuleInfo 伴生对象，提供 Java 兼容的工厂方法
 */
object SourceRuleInfo {
  
  /**
   * Java 兼容的三参数构造器
   */
  def apply(name: String, displayName: String, description: String): SourceRuleInfo = {
    new SourceRuleInfo(name, displayName, description)
  }
}
