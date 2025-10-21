package omni.scan

import io.joern.dataflowengineoss.language.Path

/**
 * 漏洞扫描结果回调接口，用于实时更新UI和处理扫描结果
 */
trait ScanResultCallback {
  /**
   * 当发现漏洞时调用
   *
   * @param path 漏洞路径
   * @param sourceQuery 源点查询
   * @param sinkQuery 汇点查询
   * @param pattern 匹配的模式
   */
  def onVulnerabilityFound(path: Path, sourceQuery: Option[Query], sinkQuery: Option[Query], pattern: String): Unit

  /**
   * 当批处理完成时调用
   *
   * @param batchIndex 当前批次索引
   * @param totalBatches 总批次数
   */
  def onBatchCompleted(batchIndex: Int, totalBatches: Int): Unit
}
