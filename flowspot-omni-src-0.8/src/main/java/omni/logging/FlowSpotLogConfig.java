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
package omni.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * FlowSpot 统一日志配置管理器
 * 确保插件和引擎使用相同的日志文件名
 */
public class FlowSpotLogConfig {
    
    private static final String LOG_FILENAME_PREFIX = "flowspot-analysis";
    private static final String LOG_FILENAME_SUFFIX = ".log";
    private static volatile String SHARED_LOG_FILENAME = null;
    
    /**
     * 获取共享的日志文件名（带时间戳）
     * 确保插件和引擎使用完全相同的文件名
     */
    public static String getSharedLogFileName() {
        if (SHARED_LOG_FILENAME == null) {
            synchronized (FlowSpotLogConfig.class) {
                if (SHARED_LOG_FILENAME == null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
                    String timestamp = dateFormat.format(new Date());
                    SHARED_LOG_FILENAME = LOG_FILENAME_PREFIX + "-" + timestamp + LOG_FILENAME_SUFFIX;
                }
            }
        }
        return SHARED_LOG_FILENAME;
    }
    
    /**
     * 重置日志文件名（用于测试或新的分析会话）
     */
    public static void resetLogFileName() {
        synchronized (FlowSpotLogConfig.class) {
            SHARED_LOG_FILENAME = null;
        }
    }
    
    /**
     * 设置自定义日志文件名（用于特殊情况）
     */
    public static void setCustomLogFileName(String fileName) {
        synchronized (FlowSpotLogConfig.class) {
            SHARED_LOG_FILENAME = fileName;
        }
    }
}
