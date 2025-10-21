/*
 * Copyright 2024 FlowSpot plugin contributors
 *
 * This file is part of IntelliJ FlowSpot plugin.
 *
 * IntelliJ FlowSpot plugin is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * IntelliJ FlowSpot plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IntelliJ FlowSpot plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.flowspot.intellij.core;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.flowspot.intellij.logging.FlowSpotLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FlowSpot 全局统一日志管理器
 * 使用 IntelliJ 平台的日志系统，避免外部依赖冲突
 */
public class FlowSpotLogManager {
    
    // 全局实例管理
    private static final ConcurrentHashMap<String, FlowSpotLogManager> instances = new ConcurrentHashMap<>();
    
    // 当前活跃的项目实例（用于静态方法）
    private static volatile FlowSpotLogManager currentProjectInstance = null;
    
    // FlowSpot 日志器 - 使用IntelliJ平台日志系统
    private static final FlowSpotLogger logger = FlowSpotLogger.getInstance("com.flowspot");
    private static volatile boolean initialized = false;
    
    private final Project project;
    private final String projectPath;
    private final String loggerName;
    
    private FlowSpotLogManager(@NotNull Project project) {
        this.project = project;
        // 统一使用用户主目录下的 .flowspot 目录
        this.projectPath = System.getProperty("user.home") + "/.flowspot";
        this.loggerName = "com.flowspot.project." + project.getName().replaceAll("[^a-zA-Z0-9]", "_");
        
        // 初始化日志配置
        initializeLogging();
        
        // 记录初始化信息
        logger.info("=== FlowSpot 项目日志初始化 ===");
        logger.info("项目名称: " + project.getName());
        logger.info("统一日志目录: " + projectPath);
        logger.info("日志器名称: " + loggerName);
        
    }
    
    /**
     * 获取项目的日志管理器实例（单例模式）
     */
    @NotNull
    public static FlowSpotLogManager getInstance(@NotNull Project project) {
        String projectKey = project.getBasePath() != null ? project.getBasePath() : project.getName();
        FlowSpotLogManager instance = instances.computeIfAbsent(projectKey, k -> new FlowSpotLogManager(project));
        
        // 设置为当前活跃实例（用于静态方法）
        currentProjectInstance = instance;
        
        return instance;
    }
    
    /**
     * 设置当前活跃的项目实例（用于静态方法调用）
     */
    public static void setCurrentProject(@NotNull Project project) {
        currentProjectInstance = getInstance(project);
    }
    
    /**
     * 获取全局日志管理器实例（用于非项目相关的日志）
     */
    @NotNull
    public static FlowSpotLogManager getGlobalInstance() {
        return instances.computeIfAbsent("global", k -> new FlowSpotLogManager());
    }
    
    /**
     * 私有构造函数（用于全局实例）
     */
    private FlowSpotLogManager() {
        this.project = null;
        this.projectPath = System.getProperty("user.home") + "/.flowspot";
        this.loggerName = "com.flowspot.global";
        
        // 初始化 Log4j2 配置
        initializeLogging();
        
        // 记录初始化信息
        logger.info("=== FlowSpot 全局日志初始化 ===");
        logger.info("全局日志路径: " + projectPath);
    }
    
    /**
     * 初始化 IntelliJ 平台日志系统
     */
    private void initializeLogging() {
        if (!initialized) {
            synchronized (FlowSpotLogManager.class) {
                if (!initialized) {
                    try {
                        // 确保日志目录存在（可选，主要用于其他文件存储）
                        Path logDirPath = Paths.get(projectPath);
                        if (!Files.exists(logDirPath)) {
                            Files.createDirectories(logDirPath);
                        }
                        
                        initialized = true;
                        
                        // 记录初始化信息
                        logger.info("FlowSpot 日志系统初始化完成，使用 IntelliJ 平台日志");
                        logger.info("配置目录: " + logDirPath.toAbsolutePath());
                        
                    } catch (IOException e) {
                        logger.error("创建配置目录失败: " + e.getMessage(), e);
                    }
                }
            }
        }
    }
    
    /**
     * 记录信息日志
     */
    public void logInfo(@NotNull String message) {
        logger.info("[" + getProjectName() + "] " + message);
    }
    
    /**
     * 记录调试日志
     */
    public void logDebug(@NotNull String message) {
        logger.debug("[" + getProjectName() + "] " + message);
    }
    
    /**
     * 记录警告日志
     */
    public void logWarning(@NotNull String message) {
        logger.warn("[" + getProjectName() + "] " + message);
    }
    
    /**
     * 记录错误日志
     */
    public void logError(@NotNull String message) {
        logger.error("[" + getProjectName() + "] " + message);
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public void logError(@NotNull String message, @NotNull Throwable throwable) {
        logger.error("[" + getProjectName() + "] " + message, throwable);
    }
    
    /**
     * 获取项目名称
     */
    private String getProjectName() {
        return project != null ? project.getName() : "Global";
    }
    
    /**
     * 静态方法：记录信息日志（优先使用当前项目实例）
     */
    public static void info(@NotNull String message) {
        if (currentProjectInstance != null) {
            currentProjectInstance.logInfo(message);
        } else {
            getGlobalInstance().logInfo(message);
        }
    }
    
    /**
     * 静态方法：记录信息日志（使用项目实例）
     */
    public static void info(@NotNull Project project, @NotNull String message) {
        getInstance(project).logInfo(message);
    }
    
    /**
     * 静态方法：记录调试日志
     */
    public static void debug(@NotNull String message) {
        if (currentProjectInstance != null) {
            currentProjectInstance.logDebug(message);
        } else {
            getGlobalInstance().logDebug(message);
        }
    }
    
    /**
     * 静态方法：记录警告日志
     */
    public static void warning(@NotNull String message) {
        if (currentProjectInstance != null) {
            currentProjectInstance.logWarning(message);
        } else {
            getGlobalInstance().logWarning(message);
        }
    }
    
    /**
     * 静态方法：记录错误日志
     */
    public static void error(@NotNull String message) {
        if (currentProjectInstance != null) {
            currentProjectInstance.logError(message);
        } else {
            getGlobalInstance().logError(message);
        }
    }
    
    /**
     * 静态方法：记录错误日志（带异常）
     */
    public static void error(@NotNull String message, @NotNull Throwable throwable) {
        if (currentProjectInstance != null) {
            currentProjectInstance.logError(message, throwable);
        } else {
            getGlobalInstance().logError(message, throwable);
        }
    }
    
    
    /**
     * 关闭日志管理器（IntelliJ 平台不需要手动关闭）
     */
    public void close() {
        logger.info("[" + getProjectName() + "] FlowSpot 日志管理器关闭");
        // IntelliJ 平台会自动管理日志，无需手动处理
    }
    
    /**
     * 关闭所有日志管理器实例
     */
    public static void closeAll() {
        logger.info("关闭所有 FlowSpot 日志管理器实例");
        for (FlowSpotLogManager manager : instances.values()) {
            manager.close();
        }
        instances.clear();
    }
    
    /**
     * 配置调试日志（使用 IntelliJ 平台统一管理）
     */
    public static void configureDebugLogging(@NotNull Project project) {
        FlowSpotLogManager logManager = getInstance(project);
        logManager.logInfo("初始化 FlowSpot 调试日志系统");
        logManager.logInfo("使用 IntelliJ 平台统一日志管理");
        logManager.logInfo("FlowSpot 调试日志系统配置完成");
    }
    
    // ========== 业务日志方法 ==========
    
    /**
     * 记录规则选择信息
     */
    public void logRuleSelection(@Nullable Set<String> sourceRules, @Nullable Set<String> sinkRules) {
        logInfo("=== Rule Selection ===");
        
        if (sourceRules != null && !sourceRules.isEmpty()) {
            logInfo("Selected Source Rules (" + sourceRules.size() + "):");
            for (String rule : sourceRules) {
                logInfo("  - " + rule);
            }
        } else {
            logInfo("No source rules selected (using all available)");
        }
        
        if (sinkRules != null && !sinkRules.isEmpty()) {
            logInfo("Selected Sink Rules (" + sinkRules.size() + "):");
            for (String rule : sinkRules) {
                logInfo("  - " + rule);
            }
        } else {
            logInfo("No sink rules selected (using all available)");
        }
    }
    
    /**
     * 记录进度信息
     */
    public void logProgress(@NotNull String message) {
        logInfo("[PROGRESS] " + message);
    }
    
    /**
     * 记录分析结果统计
     */
    public void logAnalysisResults(int bugCount, int analyzedFileCount) {
        logInfo("=== Analysis Results ===");
        logInfo("Total Bugs Found: " + bugCount);
        logInfo("Analyzed Files: " + analyzedFileCount);
    }
    
    /**
     * 记录分析完成信息
     */
    public void logAnalysisCompleted(long durationMs) {
        logInfo("=== Analysis Completed ===");
        double durationSeconds = durationMs / 1000.0;
        logInfo("Total Duration: " + durationMs + " ms (" + String.format("%.3f", durationSeconds) + " seconds)");
    }
}
