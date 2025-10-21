# FlowSpot 全局日志系统

## 概述

FlowSpot 全局日志系统为 flowspot-omni-src-0.8 项目提供了统一的日志记录功能，能够捕获和记录所有的输出信息，包括：

- `callback.updateProgress` 调用
- `println` 输出
- 系统日志信息
- 错误和异常信息

## 特性

### 🚀 **异步写入**
- 使用独立线程进行日志写入，避免IO阻塞主线程
- 内置队列缓冲，提高性能

### 📁 **自动文件管理**
- 按日期自动切换日志文件 (`flowspot-YYYY-MM-dd.log`)
- 自动创建日志目录 (`actualProjectBasePath/.flowspot/logs/`)
- 自动清理旧日志文件（默认保留7天）

### 🔄 **输出拦截**
- 自动拦截 `System.out.println` 和 `System.err.println`
- 包装进度回调，记录所有进度更新

### 📊 **多级别日志**
- DEBUG, INFO, WARN, ERROR, PROGRESS 五个日志级别
- 带时间戳、线程名和类名的详细格式

## 使用方法

### 1. 自动集成

日志系统已自动集成到 `FlowSpot.doAnalysisWithConfig` 方法中：

```scala
// 在分析开始时自动初始化
FlowSpotLogger.initialize(actualProjectBasePath)
OutputRedirector.startRedirection()

// 在分析结束时自动清理
OutputRedirector.stopRedirection()
FlowSpotLogger.shutdown()
```

### 2. 手动使用

#### 基本日志记录

```scala
import omni.util.FlowSpotLogger

// 初始化（通常在应用启动时调用一次）
FlowSpotLogger.initialize("/path/to/project")

// 记录不同级别的日志
FlowSpotLogger.debug("调试信息")
FlowSpotLogger.info("普通信息")
FlowSpotLogger.warn("警告信息")
FlowSpotLogger.error("错误信息", exception = Some(throwable))

// 记录进度信息
FlowSpotLogger.logProgress(50, 100, "处理中...")
```

#### 使用 Logging Trait

```scala
import omni.util.Logging

class MyClass extends Logging {
  def doSomething(): Unit = {
    logInfo("开始处理")
    logProgress("处理进度更新")
    logError("处理失败", Some(exception))
  }
}
```

#### 进度回调包装

```scala
import omni.util.ProgressCallbackFactory

// 创建带日志记录的进度回调
val originalCallback = new MyProgressCallback()
val loggingCallback = ProgressCallbackFactory.createLoggingCallback(originalCallback)

// 或者创建控制台回调（带日志记录）
val consoleCallback = ProgressCallbackFactory.createConsoleCallback()
```

#### 输出重定向

```scala
import omni.util.OutputRedirector

// 开始拦截 println 输出
OutputRedirector.startRedirection()

// 现在所有的 println 都会被记录到日志
println("这条消息会被记录到日志文件")

// 停止拦截
OutputRedirector.stopRedirection()
```

## 日志文件格式

```
2024-09-28 17:30:15.123 INFO     main-thread          [FlowSpot] 分析开始
2024-09-28 17:30:15.456 PROGRESS callback-thread      [ProgressCallback] Progress: 50/100 (50.0%) - 处理中...
2024-09-28 17:30:15.789 INFO     main-thread          [PRINTLN] 控制台输出信息
2024-09-28 17:30:16.012 ERROR    worker-thread        [Scanner] 扫描失败
java.lang.RuntimeException: 扫描错误
    at com.example.Scanner.scan(Scanner.java:123)
    ...
```

## 配置选项

### 日志文件位置

日志文件保存在：`{actualProjectBasePath}/.flowspot/logs/flowspot-{date}.log`

### 清理策略

```scala
// 清理7天前的日志文件（默认）
FlowSpotLogger.cleanupOldLogs(7)

// 清理30天前的日志文件
FlowSpotLogger.cleanupOldLogs(30)
```

### 队列大小

日志队列使用 `LinkedBlockingQueue`，如果队列满了会丢弃新的日志消息并输出警告。

## 性能考虑

### IO 优化
- **异步写入**：所有日志写入都在独立线程中进行
- **缓冲写入**：使用 `BufferedWriter` 减少磁盘IO
- **非阻塞队列**：使用 `offer()` 方法避免阻塞主线程

### 内存管理
- **有界队列**：防止内存溢出
- **及时刷新**：每次写入后立即刷新，确保数据持久化
- **资源清理**：程序结束时自动关闭所有资源

## 故障处理

### 日志写入失败
- 如果日志写入失败，错误信息会输出到 `System.err`
- 不会影响主程序的正常运行

### 队列满载
- 如果日志队列满了，新的日志消息会被丢弃
- 会在控制台输出警告信息

### 文件权限问题
- 如果无法创建日志目录或文件，会输出错误信息到控制台
- 日志系统会优雅降级，不影响主程序

## 示例项目结构

```
project-root/
├── .flowspot/
│   └── logs/
│       ├── flowspot-2024-09-28.log
│       ├── flowspot-2024-09-27.log
│       └── flowspot-2024-09-26.log
├── src/
└── ...
```

## 最佳实践

1. **早期初始化**：在应用启动时尽早调用 `FlowSpotLogger.initialize()`
2. **及时清理**：在应用结束时调用 `FlowSpotLogger.shutdown()`
3. **合理使用级别**：根据信息重要性选择合适的日志级别
4. **异常记录**：记录错误时总是包含异常堆栈信息
5. **避免过度日志**：在循环中避免记录过多日志信息

## 故障排除

### 日志文件未生成
- 检查项目根目录路径是否正确
- 检查文件系统权限
- 查看控制台是否有错误信息

### 日志内容不完整
- 确保在程序结束前调用了 `FlowSpotLogger.shutdown()`
- 检查是否有异常导致日志线程提前退出

### 性能问题
- 检查日志级别设置
- 考虑减少日志输出频率
- 监控磁盘空间和IO性能
