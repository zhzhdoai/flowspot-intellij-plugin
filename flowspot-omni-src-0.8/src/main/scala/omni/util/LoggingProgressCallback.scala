package omni.util

import omni.scan.ProgressCallback

/**
 * 日志记录的进度回调包装器
 */
class LoggingProgressCallback(originalCallback: ProgressCallback) extends ProgressCallback {
  
  override def updateMessage(message: String): Unit = {
    // 记录到日志
    FlowSpotLogger.info(s"[MESSAGE] $message", Some("ProgressCallback"))
    
    // 调用原始回调
    originalCallback.updateMessage(message)
  }
  
  override def updateProgress(progress: Int): Unit = {
    // 记录到日志
    FlowSpotLogger.logProgress(progress, "Progress Update")
    
    // 调用原始回调
    originalCallback.updateProgress(progress)
  }
}

/**
 * 简单的控制台进度回调实现
 */
class ConsoleProgressCallback extends ProgressCallback {
  
  override def updateMessage(message: String): Unit = {
    println(s"[MESSAGE] $message")
  }
  
  override def updateProgress(progress: Int): Unit = {
    println(s"Progress: $progress%")
  }
}

/**
 * 静默进度回调（只记录日志，不输出到控制台）
 */
class SilentProgressCallback extends ProgressCallback {
  
  override def updateMessage(message: String): Unit = {
    // 只记录到日志，不输出到控制台
    FlowSpotLogger.info(s"[MESSAGE] $message", Some("SilentCallback"))
  }
  
  override def updateProgress(progress: Int): Unit = {
    // 只记录到日志，不输出到控制台
    FlowSpotLogger.logProgress(progress, "Silent Progress")
  }
}

/**
 * 进度回调工厂
 */
object ProgressCallbackFactory {
  
  /**
   * 创建带日志记录的进度回调
   */
  def createLoggingCallback(originalCallback: ProgressCallback): ProgressCallback = {
    new LoggingProgressCallback(originalCallback)
  }
  
  /**
   * 创建控制台进度回调（带日志记录）
   */
  def createConsoleCallback(): ProgressCallback = {
    new LoggingProgressCallback(new ConsoleProgressCallback())
  }
  
  /**
   * 创建静默进度回调（只记录日志）
   */
  def createSilentCallback(): ProgressCallback = {
    new SilentProgressCallback()
  }
}
