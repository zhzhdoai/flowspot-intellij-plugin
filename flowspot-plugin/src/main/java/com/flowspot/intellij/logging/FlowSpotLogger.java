/*
 * Copyright 2024 FlowSpot plugin contributors
 *
 * This file is part of IntelliJ FlowSpot plugin.
 */

package com.flowspot.intellij.logging;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * FlowSpot专用日志适配器，使用IntelliJ平台的日志系统
 * 避免与外部Log4j依赖的冲突问题
 */
public class FlowSpotLogger {
    
    private final Logger logger;
    
    private FlowSpotLogger(@NotNull String category) {
        this.logger = Logger.getInstance(category);
    }
    
    /**
     * 获取指定类的日志器
     */
    @NotNull
    public static FlowSpotLogger getInstance(@NotNull Class<?> clazz) {
        return new FlowSpotLogger(clazz.getName());
    }
    
    /**
     * 获取指定分类的日志器
     */
    @NotNull
    public static FlowSpotLogger getInstance(@NotNull String category) {
        return new FlowSpotLogger(category);
    }
    
    /**
     * 记录调试信息
     */
    public void debug(@NotNull String message) {
        logger.debug(message);
    }
    
    public void debug(@NotNull String message, @Nullable Throwable throwable) {
        logger.debug(message, throwable);
    }
    
    /**
     * 记录信息
     */
    public void info(@NotNull String message) {
        logger.info(message);
    }
    
    public void info(@NotNull String message, @Nullable Throwable throwable) {
        logger.info(message, throwable);
    }
    
    /**
     * 记录警告
     */
    public void warn(@NotNull String message) {
        logger.warn(message);
    }
    
    public void warn(@NotNull String message, @Nullable Throwable throwable) {
        logger.warn(message, throwable);
    }
    
    /**
     * 记录错误
     */
    public void error(@NotNull String message) {
        logger.error(message);
    }
    
    public void error(@NotNull String message, @Nullable Throwable throwable) {
        logger.error(message, throwable);
    }
    
    /**
     * 检查是否启用调试日志
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
    
    /**
     * 记录分析进度（使用info级别）
     */
    public void progress(@NotNull String message) {
        logger.info("[PROGRESS] " + message);
    }
    
    /**
     * 记录分析统计信息
     */
    public void stats(@NotNull String message) {
        logger.info("[STATS] " + message);
    }
    
    /**
     * 记录配置信息
     */
    public void config(@NotNull String message) {
        logger.info("[CONFIG] " + message);
    }
}
