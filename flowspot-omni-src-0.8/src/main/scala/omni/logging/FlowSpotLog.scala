/*
 * Copyright 2024 FlowSpot plugin contributors
 *
 * This file is part of FlowSpot OMNI.
 *
 * FlowSpot OMNI is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * FlowSpot OMNI is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlowSpot OMNI.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package omni.logging

import omni.logging.FlowSpotLogger

/**
 * FlowSpot OMNI Scala 日志接口
 * 为 Scala 代码提供便捷的日志记录功能
 */
object FlowSpotLog {
  
  /**
   * 记录信息日志
   */
  def info(message: String): Unit = {
    FlowSpotLogger.info(message)
  }
  
  /**
   * 记录警告日志
   */
  def warn(message: String): Unit = {
    FlowSpotLogger.warn(message)
  }
  
  /**
   * 记录错误日志
   */
  def error(message: String): Unit = {
    FlowSpotLogger.error(message)
  }
  
  /**
   * 记录错误日志（带异常）
   */
  def error(message: String, throwable: Throwable): Unit = {
    FlowSpotLogger.error(message, throwable)
  }
  
  /**
   * 记录调试日志
   */
  def debug(message: String): Unit = {
    FlowSpotLogger.debug(message)
  }
  
  /**
   * 记录进度日志
   */
  def progress(message: String): Unit = {
    FlowSpotLogger.progress(message)
  }
  
  /**
   * 获取日志文件路径
   */
  def getLogFilePath: String = {
    FlowSpotLogger.getInstance().getLogFilePath
  }
  
  /**
   * 关闭日志记录器
   */
  def close(): Unit = {
    FlowSpotLogger.getInstance().close()
  }
}
