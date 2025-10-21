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
 * FlowSpot OMNI 简单日志记录器
 * 使用控制台输出，避免外部依赖
 */
public class FlowSpotLogger {
    
    private static FlowSpotLogger instance;
    private static final Object instanceLock = new Object();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    private FlowSpotLogger() {
        // 简单初始化
    }
    
    /**
     * 格式化日志消息
     */
    private String formatMessage(String level, String message) {
        return String.format("[%s] [%s] %s", dateFormat.format(new Date()), level, message);
    }
    
    /**
     * 获取单例实例
     */
    public static FlowSpotLogger getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new FlowSpotLogger();
                }
            }
        }
        return instance;
    }
    
    /**
     * 记录信息日志
     */
    public static void info(String message) {
        System.out.println(getInstance().formatMessage("INFO", message));
    }
    
    /**
     * 记录警告日志
     */
    public static void warn(String message) {
        System.out.println(getInstance().formatMessage("WARN", message));
    }
    
    /**
     * 记录错误日志
     */
    public static void error(String message) {
        System.err.println(getInstance().formatMessage("ERROR", message));
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public static void error(String message, Throwable throwable) {
        System.err.println(getInstance().formatMessage("ERROR", message));
        throwable.printStackTrace();
    }
    
    /**
     * 记录调试日志
     */
    public static void debug(String message) {
        System.out.println(getInstance().formatMessage("DEBUG", message));
    }
    
    /**
     * 记录进度日志
     */
    public static void progress(String message) {
        System.out.println(getInstance().formatMessage("PROGRESS", message));
    }
    
    /**
     * 获取当前日志文件名（兼容性方法）
     */
    public static String getCurrentLogFileName() {
        return "console-output";
    }
    
    /**
     * 获取日志文件路径（兼容性方法）
     */
    public String getLogFilePath() {
        return "console-output";
    }
    
    /**
     * 关闭日志记录器（兼容性方法）
     */
    public void close() {
        // 控制台输出无需关闭
    }
}
