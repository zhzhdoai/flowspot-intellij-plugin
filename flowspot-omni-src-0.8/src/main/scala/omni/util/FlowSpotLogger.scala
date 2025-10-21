package omni.util

import java.io._
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * FlowSpot 全局日志系统
 * 异步写入日志，避免IO阻塞主线程
 */
object FlowSpotLogger {
  
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
  private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  
  // 异步日志队列
  private val logQueue: BlockingQueue[LogEntry] = new LinkedBlockingQueue[LogEntry]()
  
  // 日志基础路径
  @volatile private var basePath: Option[Path] = None
  
  // 日志写入线程
  @volatile private var writerThread: Option[Thread] = None
  @volatile private var isShutdown = false
  
  // 执行上下文
  implicit private val ec: ExecutionContext = ExecutionContext.global
  
  case class LogEntry(
    level: LogLevel,
    message: String,
    timestamp: LocalDateTime,
    threadName: String,
    className: Option[String] = None,
    exception: Option[Throwable] = None
  )
  
  sealed trait LogLevel {
    def name: String
  }
  
  object LogLevel {
    case object DEBUG extends LogLevel { val name = "DEBUG" }
    case object INFO extends LogLevel { val name = "INFO" }
    case object WARN extends LogLevel { val name = "WARN" }
    case object ERROR extends LogLevel { val name = "ERROR" }
    case object PROGRESS extends LogLevel { val name = "PROGRESS" }
  }
  
  /**
   * 初始化日志系统
   */
  def initialize(actualProjectBasePath: String): Unit = {
    synchronized {
      // 如果已经初始化且路径相同，先关闭再重新初始化
      if (basePath.isDefined) {
        val currentPath = basePath.get.toString
        val newPath = Paths.get(actualProjectBasePath, ".flowspot", "logs").toString
        if (currentPath == newPath) {
          // 路径相同，但需要确保写入线程正常运行
          if (writerThread.isEmpty || !writerThread.get.isAlive) {
            info("Restarting logger writer thread for new analysis")
            startWriterThread()
          }
          return
        } else {
          // 路径不同，关闭现有的再重新初始化
          shutdown()
        }
      }
      
      try {
        val logDir = Paths.get(actualProjectBasePath, ".flowspot", "logs")
        Files.createDirectories(logDir)
        basePath = Some(logDir)
        isShutdown = false
        
        // 启动异步写入线程
        startWriterThread()
        
        info(s"FlowSpot Logger initialized, logs will be saved to: ${logDir.toAbsolutePath}")
      } catch {
        case e: Exception =>
          System.err.println(s"Failed to initialize FlowSpot Logger: ${e.getMessage}")
          e.printStackTrace()
      }
    }
  }
  
  /**
   * 启动日志写入线程
   */
  private def startWriterThread(): Unit = {
    // 先停止现有线程（如果存在）
    writerThread.foreach { thread =>
      if (thread.isAlive) {
        thread.interrupt()
        try {
          thread.join(1000) // 等待1秒
        } catch {
          case _: InterruptedException => Thread.currentThread().interrupt()
        }
      }
    }
    
    writerThread = Some(new Thread(new Runnable {
      override def run(): Unit = {
        var currentWriter: Option[BufferedWriter] = None
        var currentDate: String = ""
        
        try {
          while (!isShutdown || !logQueue.isEmpty) {
            try {
              val entry = logQueue.take()
              
              // 检查是否需要切换日志文件（按日期）
              val entryDate = entry.timestamp.format(fileNameFormatter)
              if (entryDate != currentDate) {
                // 关闭旧的writer
                currentWriter.foreach(_.close())
                
                // 创建新的writer
                currentDate = entryDate
                val logFile = basePath.get.resolve(s"flowspot-$entryDate.log")
                currentWriter = Some(Files.newBufferedWriter(
                  logFile, 
                  StandardOpenOption.CREATE, 
                  StandardOpenOption.APPEND
                ))
              }
              
              // 写入日志
              currentWriter.foreach { writer =>
                val logLine = formatLogEntry(entry)
                writer.write(logLine)
                writer.newLine()
                writer.flush() // 确保及时写入
              }
              
            } catch {
              case _: InterruptedException =>
                // 线程被中断，退出循环
                Thread.currentThread().interrupt()
                return
              case e: Exception =>
                // 日志写入失败，输出到控制台
                System.err.println(s"Failed to write log: ${e.getMessage}")
            }
          }
        } finally {
          // 清理资源
          currentWriter.foreach(_.close())
        }
      }
    }, "FlowSpot-Logger-Thread"))
    
    writerThread.foreach { thread =>
      thread.setDaemon(true)
      thread.start()
    }
  }
  
  /**
   * 格式化日志条目
   */
  private def formatLogEntry(entry: LogEntry): String = {
    val timestamp = entry.timestamp.format(dateFormatter)
    val level = entry.level.name.padTo(8, ' ')
    val thread = entry.threadName.padTo(20, ' ')
    val className = entry.className.map(c => s"[$c] ").getOrElse("")
    
    val message = entry.exception match {
      case Some(ex) =>
        val sw = new StringWriter()
        ex.printStackTrace(new PrintWriter(sw))
        s"${entry.message}\n${sw.toString}"
      case None => entry.message
    }
    
    s"$timestamp $level $thread $className$message"
  }
  
  /**
   * 异步记录日志
   */
  private def log(level: LogLevel, message: String, className: Option[String] = None, exception: Option[Throwable] = None): Unit = {
    if (basePath.isDefined) {
      val entry = LogEntry(
        level = level,
        message = message,
        timestamp = LocalDateTime.now(),
        threadName = Thread.currentThread().getName,
        className = className,
        exception = exception
      )
      
      // 非阻塞方式添加到队列
      if (!logQueue.offer(entry)) {
        // 队列满了，输出警告到控制台
        System.err.println(s"Log queue is full, dropping log message: $message")
      }
    }
  }
  
  // 公共日志方法
  def debug(message: String, className: Option[String] = None): Unit = 
    log(LogLevel.DEBUG, message, className)
  
  def info(message: String, className: Option[String] = None): Unit = 
    log(LogLevel.INFO, message, className)
  
  def warn(message: String, className: Option[String] = None): Unit = 
    log(LogLevel.WARN, message, className)
  
  def error(message: String, className: Option[String] = None, exception: Option[Throwable] = None): Unit = 
    log(LogLevel.ERROR, message, className, exception)
  
  def progress(message: String, className: Option[String] = None): Unit = 
    log(LogLevel.PROGRESS, message, className)
  
  /**
   * 记录进度更新（专门用于callback.updateProgress）
   */
  def logProgress(progressValue: Int, message: String = ""): Unit = {
    val progressMsg = if (message.nonEmpty) {
      s"Progress: $progressValue% - $message"
    } else {
      s"Progress: $progressValue%"
    }
    progress(progressMsg, Some("ProgressCallback"))
  }
  
  /**
   * 记录进度更新（带总数）
   */
  def logProgressWithTotal(current: Int, total: Int, message: String = ""): Unit = {
    val percentage = if (total > 0) f"${(current * 100.0 / total)}%.1f" else "0.0"
    val progressMsg = if (message.nonEmpty) {
      s"Progress: $current/$total ($percentage%) - $message"
    } else {
      s"Progress: $current/$total ($percentage%)"
    }
    progress(progressMsg, Some("ProgressCallback"))
  }
  
  /**
   * 记录println输出
   */
  def logPrintln(message: String, className: Option[String] = None): Unit = {
    info(s"[PRINTLN] $message", className)
  }
  
  /**
   * 关闭日志系统
   */
  def shutdown(): Unit = {
    synchronized {
      isShutdown = true
      
      // 中断写入线程
      writerThread.foreach(_.interrupt())
      
      // 等待线程结束（最多等待5秒）
      writerThread.foreach { thread =>
        try {
          thread.join(5000)
        } catch {
          case _: InterruptedException =>
            Thread.currentThread().interrupt()
        }
      }
      
      // 重置状态
      writerThread = None
      basePath = None
      
      info("FlowSpot Logger shutdown completed")
    }
  }
  
  /**
   * 获取当前日志文件路径
   */
  def getCurrentLogFile: Option[Path] = {
    basePath.map { base =>
      val today = LocalDateTime.now().format(fileNameFormatter)
      base.resolve(s"flowspot-$today.log")
    }
  }
  
  /**
   * 清理旧日志文件（保留最近N天）
   */
  def cleanupOldLogs(keepDays: Int = 7): Unit = {
    basePath.foreach { logDir =>
      Future {
        try {
          val cutoffDate = LocalDateTime.now().minusDays(keepDays)
          val cutoffDateStr = cutoffDate.format(fileNameFormatter)
          
          Files.list(logDir)
            .filter(_.getFileName.toString.startsWith("flowspot-"))
            .filter(_.getFileName.toString.endsWith(".log"))
            .filter { file =>
              val fileName = file.getFileName.toString
              val dateStr = fileName.substring(9, 19) // extract date part
              dateStr < cutoffDateStr
            }
            .forEach { file =>
              try {
                Files.delete(file)
                info(s"Deleted old log file: ${file.getFileName}")
              } catch {
                case e: Exception =>
                  warn(s"Failed to delete old log file ${file.getFileName}: ${e.getMessage}")
              }
            }
        } catch {
          case e: Exception =>
            error(s"Failed to cleanup old logs: ${e.getMessage}", exception = Some(e))
        }
      }
    }
  }
}

/**
 * 日志记录trait，可以被其他类混入使用
 */
trait Logging {
  protected def logger: FlowSpotLogger.type = FlowSpotLogger
  
  protected def logDebug(message: String): Unit = 
    logger.debug(message, Some(this.getClass.getSimpleName))
  
  protected def logInfo(message: String): Unit = 
    logger.info(message, Some(this.getClass.getSimpleName))
  
  protected def logWarn(message: String): Unit = 
    logger.warn(message, Some(this.getClass.getSimpleName))
  
  protected def logError(message: String, exception: Option[Throwable] = None): Unit = 
    logger.error(message, Some(this.getClass.getSimpleName), exception)
  
  protected def logProgress(message: String): Unit = 
    logger.progress(message, Some(this.getClass.getSimpleName))
}
