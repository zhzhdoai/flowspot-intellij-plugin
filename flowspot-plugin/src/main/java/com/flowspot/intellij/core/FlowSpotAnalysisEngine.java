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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import com.flowspot.intellij.model.FlowSpotVulnerability;
import com.flowspot.intellij.model.FlowSpotVulnerabilityCollection;
import com.flowspot.intellij.service.FlowSpotResultsPublisher;
import com.flowspot.intellij.service.FlowSpotFilterManager;
import omni.flowspot.core.FlowSpotBugInstance;
import omni.flowspot.core.FlowSpotBugCollection;
import omni.scan.FlowSpotProjectConfig;
import omni.scan.FlowSpot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FlowSpot 独立分析引擎
 * 不依赖 SpotBugs，直接使用 FlowSpot 核心分析库
 */
public class FlowSpotAnalysisEngine {
    
    private final Project project;
    private final FlowSpotLogManager logManager;
    private final MessageBus messageBus;
    private final FlowSpotFilterManager filterManager;
    private final FlowSpotConfigManager configManager;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);
    
    public FlowSpotAnalysisEngine(@NotNull Project project) {
        this.project = project;
        this.logManager = FlowSpotLogManager.getInstance(project);
        this.configManager = new FlowSpotConfigManager(project);
        this.filterManager = FlowSpotFilterManager.getInstance(project);
        this.messageBus = project.getMessageBus();
        
        // 设置当前项目实例，确保静态日志方法使用正确的项目目录
        FlowSpotLogManager.setCurrentProject(project);
        
        // 记录使用的统一日志文件路径
        logManager.logInfo("=== FlowSpot 插件初始化 ===");
        logManager.logInfo("使用 IntelliJ 平台日志系统");
    }
    
    /**
     * 异步执行 FlowSpot 分析
     */
    @NotNull
    public CompletableFuture<FlowSpotVulnerabilityCollection> analyzeAsync(
            @NotNull List<String> sourcePaths,
            @NotNull List<String> classPaths,
            @Nullable Set<String> selectedSourceRules,
            @Nullable Set<String> selectedSinkRules) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analyze(sourcePaths, classPaths, selectedSourceRules, selectedSinkRules);
            } catch (Exception e) {
                logManager.logError("Analysis failed: " + e.getMessage(), e);
                throw new RuntimeException("FlowSpot analysis failed", e);
            }
        });
    }
    
    /**
     * 同步执行 FlowSpot 分析
     */
    @NotNull
    public FlowSpotVulnerabilityCollection analyze(
            @NotNull List<String> sourcePaths,
            @NotNull List<String> classPaths,
            @Nullable Set<String> selectedSourceRules,
            @Nullable Set<String> selectedSinkRules) {
        
        // 使用默认优化配置
        return analyze(sourcePaths, classPaths, selectedSourceRules, selectedSinkRules, 
                      com.flowspot.intellij.model.OptimizationConfig.createDefault());
    }
    
    /**
     * 同步执行 FlowSpot 分析（带优化配置）
     */
    @NotNull
    public FlowSpotVulnerabilityCollection analyze(
            @NotNull List<String> sourcePaths,
            @NotNull List<String> classPaths,
            @Nullable Set<String> selectedSourceRules,
            @Nullable Set<String> selectedSinkRules,
            @NotNull com.flowspot.intellij.model.OptimizationConfig optimizationConfig) {
        
        if (!isRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Analysis is already running");
        }
        
        isCancelled.set(false);
        
        // 确定分析项目路径
        String analysisProjectPath = determineAnalysisProjectPath(sourcePaths);
        
        // 准备分析环境（配置在项目根目录，分析目标为用户选择的目录）
        prepareAnalysisEnvironment(analysisProjectPath);
        
        // 清除之前的分析结果
        clearPreviousResults();
        
        // 获取基于分析路径的有效sinks.json配置路径
        String effectiveSinksJsonPath = configManager.getEffectiveSinksJsonPath(analysisProjectPath);
        logManager.logInfo("Using sinks.json: " + effectiveSinksJsonPath);
        
        logManager.logRuleSelection(selectedSourceRules, selectedSinkRules);
        
        long startTime = System.currentTimeMillis();
        
        try {
            logManager.logInfo("Starting FlowSpot analysis for project: " + project.getName());
            
            // 配置调试日志
            FlowSpotLogManager.configureDebugLogging(project);
            
            // 创建 FlowSpot 项目配置
            FlowSpotProjectConfig projectConfig = createProjectConfig(sourcePaths, classPaths, 
                                                                    selectedSourceRules, selectedSinkRules, optimizationConfig);
            
            // 执行 FlowSpot 分析
            logManager.logProgress("Running FlowSpot vulnerability analysis...");
            List<FlowSpotBugInstance> bugInstances = executeFlowSpotAnalysis(projectConfig);
            
            if (isCancelled.get()) {
                logManager.logInfo("Analysis was cancelled by user");
                String analysisPath = determineAnalysisProjectPath(sourcePaths);
                return new FlowSpotVulnerabilityCollection(project.getName(), analysisPath);
            }
            
            // 转换结果
            String analysisPath = determineAnalysisProjectPath(sourcePaths);
            FlowSpotVulnerabilityCollection collection = convertResults(bugInstances, analysisPath);
            
            // 应用过滤规则
            FlowSpotVulnerabilityCollection filteredCollection = applyFilters(collection, analysisProjectPath);
            
            long duration = System.currentTimeMillis() - startTime;
            logManager.logAnalysisResults(filteredCollection.getTotalCount(), sourcePaths.size());
            logManager.logAnalysisCompleted(duration);
            
            // 发布结果到消息总线
            publishResults(filteredCollection);
            
            return filteredCollection;
            
        } catch (Exception e) {
            logManager.logError("FlowSpot analysis failed", e);
            throw new RuntimeException("FlowSpot analysis failed", e);
        } finally {
            isRunning.set(false);
            logManager.close();
            configManager.dispose();
        }
    }
    
    /**
     * 取消正在运行的分析
     */
    public void cancel() {
        isCancelled.set(true);
        logManager.logInfo("Analysis cancellation requested");
    }
    
    /**
     * 检查是否正在运行
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return isCancelled.get();
    }
    
    /**
     * 准备分析环境：创建.flowspot目录和准备配置文件
     * @param analysisTargetPath 用户选择的分析目标目录
     */
    private void prepareAnalysisEnvironment(@NotNull String analysisTargetPath) {
        // 统一使用项目根目录管理配置
        String projectBasePath = project.getBasePath() != null ? project.getBasePath() : analysisTargetPath;
        File flowspotDir = new File(projectBasePath, ".flowspot");
        File configDir = new File(flowspotDir, "config");
        
        logManager.logInfo("准备分析环境");
        logManager.logInfo("  分析目标目录: " + analysisTargetPath);
        logManager.logInfo("  配置管理目录: " + projectBasePath + "/.flowspot");
        logManager.logInfo("  统一日志目录: " + System.getProperty("user.home") + "/.flowspot");
        
        // 设置 FlowSpot 核心日志系统使用统一日志目录
        try {
            // 设置系统属性，让 FlowSpot 核心引擎使用相同的日志目录
            String unifiedLogDir = System.getProperty("user.home") + "/.flowspot";
            System.setProperty("omni.log.dir", unifiedLogDir);
            System.setProperty("flowspot.core.log.dir", unifiedLogDir);
            
            logManager.logInfo("已设置 FlowSpot 核心日志使用统一目录: " + unifiedLogDir);
        } catch (Exception e) {
            logManager.logWarning("设置 FlowSpot 核心日志目录时出现异常: " + e.getMessage());
        }
        
        // 创建 .flowspot 目录
        if (!flowspotDir.exists()) {
            boolean created = flowspotDir.mkdirs();
            if (created) {
                logManager.logInfo("创建 .flowspot 目录: " + flowspotDir.getAbsolutePath());
            } else {
                logManager.logWarning("无法创建 .flowspot 目录: " + flowspotDir.getAbsolutePath());
            }
        } else {
            logManager.logInfo(".flowspot 目录已存在: " + flowspotDir.getAbsolutePath());
        }
        
        // 创建 config 目录
        if (!configDir.exists()) {
            boolean created = configDir.mkdirs();
            if (created) {
                logManager.logInfo("创建 config 目录: " + configDir.getAbsolutePath());
            } else {
                logManager.logWarning("无法创建 config 目录: " + configDir.getAbsolutePath());
            }
        } else {
            logManager.logInfo("config 目录已存在: " + configDir.getAbsolutePath());
        }
        
        // 准备 sinks.json 配置文件
        prepareSinksJsonConfig(configDir, analysisTargetPath);
    }
    
    /**
     * 准备 sinks.json 配置文件
     * @param configDir 项目根目录下的配置目录
     * @param analysisTargetPath 用户选择的分析目标目录（用于日志记录）
     */
    private void prepareSinksJsonConfig(@NotNull File configDir, @NotNull String analysisTargetPath) {
        File targetSinksJson = new File(configDir, "sinks.json");
        
        if (!targetSinksJson.exists()) {
            // 在项目根目录创建默认配置
            try {
                configManager.createDefaultSinksJson(targetSinksJson.getAbsolutePath());
                logManager.logInfo("在项目根目录创建默认 sinks.json: " + targetSinksJson.getAbsolutePath());
                
            } catch (Exception e) {
                logManager.logError("准备 sinks.json 配置文件失败: " + e.getMessage(), e);
                // 使用默认配置作为后备
                try {
                    configManager.createDefaultSinksJson(targetSinksJson.getAbsolutePath());
                } catch (Exception ex) {
                    logManager.logError("创建默认 sinks.json 也失败: " + ex.getMessage(), ex);
                }
            }
        } else {
            logManager.logInfo("sinks.json 配置文件已存在: " + targetSinksJson.getAbsolutePath());
        }
    }
    
    /**
     * 确定分析项目路径
     * 如果分析的是单个目录，则使用该目录作为项目路径
     * 如果分析的是多个文件/目录，则使用它们的公共父目录
     */
    @NotNull
    private String determineAnalysisProjectPath(@NotNull List<String> sourcePaths) {
        if (sourcePaths.isEmpty()) {
            return project.getBasePath() != null ? project.getBasePath() : "";
        }

        if (sourcePaths.size() == 1) {
            String singlePath = sourcePaths.get(0);
            File file = new File(singlePath);
            
            if (file.isDirectory()) {
                // 如果是目录，直接使用该目录
                logManager.logInfo("分析目标是单个目录: " + singlePath);
                return singlePath;
            } else {
                // 如果是文件，使用其父目录
                String parentDir = file.getParent();
                logManager.logInfo("分析目标是单个文件，使用父目录: " + parentDir);
                return parentDir != null ? parentDir : project.getBasePath();
            }
        } else {
            // 多个路径，找到公共父目录
            String commonParent = findCommonParentDirectory(sourcePaths);
            logManager.logInfo("分析目标是多个路径，使用公共父目录: " + commonParent);
            return commonParent;
        }
    }
    
    /**
     * 找到多个路径的公共父目录
     */
    @NotNull
    private String findCommonParentDirectory(@NotNull List<String> paths) {
        if (paths.isEmpty()) {
            return project.getBasePath() != null ? project.getBasePath() : "";
        }
        
        String commonParent = paths.get(0);
        for (String path : paths) {
            commonParent = findCommonParent(commonParent, path);
        }
        
        // 确保公共父目录是一个目录
        File commonFile = new File(commonParent);
        if (!commonFile.isDirectory()) {
            commonParent = commonFile.getParent();
        }
        
        return commonParent != null ? commonParent : project.getBasePath();
    }
    
    /**
     * 找到两个路径的公共父目录
     */
    @NotNull
    private String findCommonParent(@NotNull String path1, @NotNull String path2) {
        String[] parts1 = path1.split(File.separator);
        String[] parts2 = path2.split(File.separator);
        
        StringBuilder commonPath = new StringBuilder();
        int minLength = Math.min(parts1.length, parts2.length);
        
        for (int i = 0; i < minLength; i++) {
            if (parts1[i].equals(parts2[i])) {
                if (commonPath.length() > 0) {
                    commonPath.append(File.separator);
                }
                commonPath.append(parts1[i]);
            } else {
                break;
            }
        }
        
        return commonPath.toString();
    }
    
    /**
     * 创建 FlowSpot 项目配置
     */
    @NotNull
    private FlowSpotProjectConfig createProjectConfig(@NotNull List<String> sourcePaths,
                                                     @NotNull List<String> classPaths,
                                                     @Nullable Set<String> selectedSourceRules,
                                                     @Nullable Set<String> selectedSinkRules,
                                                     @NotNull com.flowspot.intellij.model.OptimizationConfig optimizationConfig) {
        
        // 确定分析目标路径和项目根目录
        String analysisTargetPath = determineAnalysisProjectPath(sourcePaths);
        String baseProjectPath = project.getBasePath() != null ? project.getBasePath() : analysisTargetPath;
        String projectName = project.getName() + "_analysis";
        
        // 创建规则集合
        java.util.Set<String> sourceRuleSet = new java.util.HashSet<>();
        java.util.Set<String> sinkRuleSet = new java.util.HashSet<>();
        
        if (selectedSourceRules != null) {
            sourceRuleSet.addAll(selectedSourceRules);
        }
        if (selectedSinkRules != null) {
            sinkRuleSet.addAll(selectedSinkRules);
        }
        
        // 创建 FlowSpotProjectConfig
        FlowSpotProjectConfig config = FlowSpotProjectConfig.create(
            analysisTargetPath,  // 使用分析目标路径作为主路径
            projectName,
            false, // enableDecompile
            "balanced", // scanMode
            sourceRuleSet,
            sinkRuleSet
        );
        
        // 设置路径配置
        config.getFlowSpotProject().setAnalysisTargetPath(analysisTargetPath);
        config.getFlowSpotProject().setBaseProjectPath(baseProjectPath);
        
        // 设置优化配置（转换为Scala配置）
        config.getFlowSpotProject().setOptimizationConfig(optimizationConfig.toScalaConfig());
        
        return config;
    }
    
    /**
     * 执行 FlowSpot 分析
     */
    @NotNull
    private List<FlowSpotBugInstance> executeFlowSpotAnalysis(@NotNull FlowSpotProjectConfig projectConfig) {
        
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        
        try {
            if (indicator != null) {
                indicator.setText("Initializing FlowSpot analysis...");
                indicator.setFraction(0.1);
            }
            
            // 检查取消状态
            if (isCancelled.get()) {
                return new ArrayList<>();
            }
            
            if (indicator != null) {
                indicator.setText("Running FlowSpot vulnerability analysis...");
                indicator.setFraction(0.3);
            }
            
            // 创建进度回调 - ProgressCallback 是一个独立的 trait
            omni.scan.ProgressCallback progressCallback = new omni.scan.ProgressCallback() {
                @Override
                public void updateMessage(String message) {
                    logManager.logProgress(message);
                    if (indicator != null) {
                        indicator.setText(message);
                    }
                }
                
                @Override
                public void updateProgress(int progress) {
                    logManager.logProgress("Progress: " + progress + "%");
                    if (indicator != null) {
                        indicator.setFraction(0.3 + (progress * 0.6 / 100.0)); // 30% to 90%
                    }
                }
            };
            
            // 使用异步线程执行FlowSpot分析
            logManager.logInfo("Starting FlowSpot analysis with async execution");
            
            // 使用CountDownLatch等待异步任务完成
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicReference<FlowSpotBugCollection> resultRef = 
                new java.util.concurrent.atomic.AtomicReference<>();
            final java.util.concurrent.atomic.AtomicReference<Exception> exceptionRef = 
                new java.util.concurrent.atomic.AtomicReference<>();
            
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    logManager.logInfo("Async thread started for FlowSpot analysis");
                    FlowSpotBugCollection result = FlowSpot.doAnalysis(projectConfig, progressCallback);
                    resultRef.set(result);
                    logManager.logInfo("Async FlowSpot analysis completed successfully");
                } catch (Exception e) {
                    exceptionRef.set(e);
                    logManager.logError("Async FlowSpot analysis failed", e);
                } finally {
                    latch.countDown();
                }
            });
            
            // 等待异步任务完成
            try {
                logManager.logInfo("Waiting for async analysis to complete...");
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Analysis was interrupted", e);
            }
            
            // 检查是否有异常
            if (exceptionRef.get() != null) {
                throw new RuntimeException("FlowSpot analysis failed", exceptionRef.get());
            }
            
            FlowSpotBugCollection bugCollection = resultRef.get();
            
            // 检查取消状态
            if (isCancelled.get()) {
                return new ArrayList<>();
            }
            
            if (indicator != null) {
                indicator.setText("Processing FlowSpot analysis results...");
                indicator.setFraction(0.90);
            }
            
            // 提取并处理 FlowSpotBugInstance 结果
            List<FlowSpotBugInstance> results = extractFlowSpotResults(bugCollection, logManager);
            
            if (indicator != null) {
                indicator.setText("Analysis completed successfully");
                indicator.setFraction(1.0);
            }
            
            logManager.logInfo("FlowSpot analysis completed. Found " + results.size() + " vulnerabilities.");
            logManager.logInfo("Vulnerability types found: " + getVulnerabilityTypesStats(results));
            
            return results;
            
        } catch (Exception e) {
            logManager.logError("FlowSpot analysis execution failed", e);
            throw new RuntimeException("Failed to execute FlowSpot analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从 FlowSpotBugCollection 中提取 FlowSpotBugInstance 结果
     */
    @NotNull
    private List<FlowSpotBugInstance> extractFlowSpotResults(@NotNull FlowSpotBugCollection bugCollection, 
                                                           @NotNull FlowSpotLogManager logManager) {
        List<FlowSpotBugInstance> results = new ArrayList<>();
        
        logManager.logInfo("Processing FlowSpotBugCollection with " + bugCollection.size() + " total bug instances");
        
        // 直接从 FlowSpotBugCollection 获取所有 FlowSpotBugInstance
        for (FlowSpotBugInstance bugInstance : bugCollection.getCollection()) {
            try {
                results.add(bugInstance);
                
                // 记录详细的漏洞信息
                logManager.logInfo("Found FlowSpot vulnerability: " + 
                    "Type=" + bugInstance.getType() + 
                    ", Priority=" + bugInstance.getPriority() + 
                    ", Annotations=" + bugInstance.getAnnotations().size());
            } catch (Exception e) {
                logManager.logError("Error processing FlowSpotBugInstance: " + bugInstance.getType(), e);
            }
        }
        
        logManager.logInfo("Successfully extracted " + results.size() + " FlowSpot vulnerabilities");
        
        // 记录类型统计信息
        Map<String, Integer> typeStats = bugCollection.getTypeStatistics();
        if (!typeStats.isEmpty()) {
            logManager.logInfo("Vulnerability type statistics: " + typeStats);
        }
        
        // 记录优先级统计信息
        Map<Integer, Integer> priorityStats = bugCollection.getPriorityStatistics();
        if (!priorityStats.isEmpty()) {
            logManager.logInfo("Priority statistics: " + priorityStats);
        }
        
        return results;
    }
    
    /**
     * 获取漏洞类型统计信息
     */
    @NotNull
    private String getVulnerabilityTypesStats(@NotNull List<FlowSpotBugInstance> vulnerabilities) {
        if (vulnerabilities.isEmpty()) {
            return "No vulnerabilities found";
        }
        
        Map<String, Integer> typeStats = new HashMap<>();
        for (FlowSpotBugInstance vuln : vulnerabilities) {
            String type = vuln.getType();
            typeStats.put(type, typeStats.getOrDefault(type, 0) + 1);
        }
        
        StringBuilder stats = new StringBuilder();
        for (Map.Entry<String, Integer> entry : typeStats.entrySet()) {
            if (stats.length() > 0) {
                stats.append(", ");
            }
            stats.append(entry.getKey()).append("(").append(entry.getValue()).append(")");
        }
        
        return stats.toString();
    }
    
    /**
     * 转换分析结果为 UI 数据模型
     */
    @NotNull
    private FlowSpotVulnerabilityCollection convertResults(@NotNull List<FlowSpotBugInstance> bugInstances, @Nullable String analysisBasePath) {
        
        // 使用新的独立结果处理器
        FlowSpotVulnerabilityCollection collection = FlowSpotResultProcessor.processResults(
            bugInstances, project.getName(), analysisBasePath);
        
        logManager.logInfo("Converted " + bugInstances.size() + " bug instances to " + 
                          collection.getTotalCount() + " vulnerabilities");
        
        return collection;
    }
    
    /**
     * 应用过滤规则到漏洞集合
     */
    private FlowSpotVulnerabilityCollection applyFilters(@NotNull FlowSpotVulnerabilityCollection collection, 
                                                        @NotNull String analysisPath) {
        // 基于分析路径加载过滤规则
        filterManager.loadFilterRules(analysisPath);
        
        int originalCount = collection.getTotalCount();
        if (originalCount == 0) {
            logManager.logInfo("No vulnerabilities to filter");
            return collection;
        }
        
        // 应用过滤规则
        FlowSpotVulnerabilityCollection filteredCollection = filterManager.applyFilters(collection);
        
        int filteredCount = filteredCollection.getTotalCount();
        int removedCount = originalCount - filteredCount;
        
        if (removedCount > 0) {
            logManager.logInfo("Filter rules applied:");
            logManager.logInfo("  Original vulnerabilities: " + originalCount);
            logManager.logInfo("  Filtered out: " + removedCount);
            logManager.logInfo("  Remaining vulnerabilities: " + filteredCount);
            logManager.logInfo("  Active filter rules: " + filterManager.getFilterRuleCount());
        } else {
            logManager.logInfo("No vulnerabilities were filtered out by current rules");
        }
        
        return filteredCollection;
    }
    
    /**
     * 发布分析结果到消息总线
     */
    private void publishResults(@NotNull FlowSpotVulnerabilityCollection collection) {
        ApplicationManager.getApplication().invokeLater(() -> {
            MessageBus messageBus = project.getMessageBus();
            FlowSpotResultsPublisher publisher = messageBus.syncPublisher(FlowSpotResultsPublisher.TOPIC);
            publisher.onFlowSpotResultsAvailable(collection);
        });
    }
    
    /**
     * 清理之前的分析结果，确保每次分析都是全新的
     */
    private void clearPreviousResults() {
        logManager.logInfo("Clearing previous analysis results...");
        
        // 发布空的结果集合来清理UI（不需要分析路径）
        ApplicationManager.getApplication().invokeLater(() -> {
            FlowSpotVulnerabilityCollection emptyCollection = new FlowSpotVulnerabilityCollection(
                project.getName());
            
            MessageBus messageBus = project.getMessageBus();
            FlowSpotResultsPublisher publisher = messageBus.syncPublisher(FlowSpotResultsPublisher.TOPIC);
            publisher.onFlowSpotResultsAvailable(emptyCollection);
        });
        
        // 清理任何可能的缓存状态
        System.gc(); // 建议垃圾回收，清理内存
        
        logManager.logInfo("Previous results cleared, ready for new analysis");
    }
}
