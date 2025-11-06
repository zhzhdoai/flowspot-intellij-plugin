package omni.util

import java.io.{OutputStream, PrintStream}

/**
 * 自定义PrintStream，用于拦截println输出并记录到日志
 */
class LoggingPrintStream(originalStream: PrintStream, loggerName: String) extends PrintStream(originalStream) {
  
  override def println(x: String): Unit = {
    // 记录到日志
    FlowSpotLogger.logPrintln(x, Some(loggerName))
    // 同时输出到原始流
    originalStream.println(x)
  }
  
  override def println(x: Any): Unit = {
    val message = if (x != null) x.toString else "null"
    FlowSpotLogger.logPrintln(message, Some(loggerName))
    originalStream.println(x)
  }
  
  override def println(): Unit = {
    FlowSpotLogger.logPrintln("", Some(loggerName))
    originalStream.println()
  }
  
  override def print(x: String): Unit = {
    // 对于print方法，我们也记录，但标记为部分消息
    FlowSpotLogger.logPrintln(s"[PARTIAL] $x", Some(loggerName))
    originalStream.print(x)
  }
  
  override def print(x: Any): Unit = {
    val message = if (x != null) x.toString else "null"
    FlowSpotLogger.logPrintln(s"[PARTIAL] $message", Some(loggerName))
    originalStream.print(x)
  }
  
  override def printf(format: String, args: Object*): PrintStream = {
    val message = String.format(format, args: _*)
    FlowSpotLogger.logPrintln(s"[PRINTF] $message", Some(loggerName))
    originalStream.printf(format, args: _*)
  }
}

/**
 * 日志输出重定向工具
 */
object OutputRedirector {
  
  private var originalOut: PrintStream = _
  private var originalErr: PrintStream = _
  private var isRedirected = false
  
  /**
   * 开始重定向System.out和System.err到日志
   */
  def startRedirection(): Unit = {
    synchronized {
      if (!isRedirected) {
        // 保存原始流
        originalOut = System.out
        originalErr = System.err
        
        // 创建日志记录流
        val loggingOut = new LoggingPrintStream(originalOut, "STDOUT")
        val loggingErr = new LoggingPrintStream(originalErr, "STDERR")
        
        // 重定向
        System.setOut(loggingOut)
        System.setErr(loggingErr)
        
        isRedirected = true
        
        FlowSpotLogger.info("Output redirection started - println calls will be logged")
      }
    }
  }
  
  /**
   * 停止重定向，恢复原始输出流
   */
  def stopRedirection(): Unit = {
    synchronized {
      if (isRedirected) {
        System.setOut(originalOut)
        System.setErr(originalErr)
        isRedirected = false
        
        FlowSpotLogger.info("Output redirection stopped")
      }
    }
  }
  
  /**
   * 检查是否正在重定向
   */
  def isOutputRedirected: Boolean = isRedirected
}
