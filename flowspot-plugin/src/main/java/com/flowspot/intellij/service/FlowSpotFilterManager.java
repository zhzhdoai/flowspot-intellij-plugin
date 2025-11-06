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
package com.flowspot.intellij.service;

import com.flowspot.intellij.core.FlowSpotLogManager;
import com.flowspot.intellij.model.FlowSpotFilterRule;
import com.flowspot.intellij.model.FlowSpotVulnerability;
import com.flowspot.intellij.model.FlowSpotVulnerabilityCollection;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FlowSpot 过滤规则管理器
 * 负责过滤规则的持久化存储、加载和应用
 */
@Service(Service.Level.PROJECT)
public final class FlowSpotFilterManager {
    
    private static final String FILTER_FILE_NAME = "filter.txt";
    private static final String FILTER_FILE_HEADER = "# FlowSpot Filter Rules";
    private static final String FILTER_FILE_FORMAT = "# Format: FilterType|ClassName|MethodName|Description|CreatedTime";
    
    private final Project project;
    private final Set<FlowSpotFilterRule> filterRules;
    private final FlowSpotLogManager logManager;
    private Path filterFilePath;
    
    public FlowSpotFilterManager(@NotNull Project project) {
        this.project = project;
        this.filterRules = ConcurrentHashMap.newKeySet();
        this.logManager = FlowSpotLogManager.getInstance(project);
        this.filterFilePath = getFilterFilePath();
        
        // 初始化时加载过滤规则
        loadFilterRules();
    }
    
    /**
     * 获取项目级别的过滤管理器实例
     */
    @NotNull
    public static FlowSpotFilterManager getInstance(@NotNull Project project) {
        return project.getService(FlowSpotFilterManager.class);
    }
    
    /**
     * 获取过滤文件路径
     */
    @NotNull
    private Path getFilterFilePath() {
        return getFilterFilePath(null);
    }
    
    /**
     * 获取基于分析路径的过滤文件路径
     * 
     * @param analysisPath 分析路径，如果为null则使用项目根目录（现在统一使用项目根目录）
     */
    @NotNull
    private Path getFilterFilePath(@Nullable String analysisPath) {
        // 统一使用项目根目录管理过滤规则
        String basePath = project.getBasePath();
        if (basePath == null) {
            basePath = System.getProperty("user.dir");
        }
        
        Path flowspotDir = Paths.get(basePath, ".flowspot");
        try {
            Files.createDirectories(flowspotDir);
        } catch (IOException e) {
            logManager.logError("Failed to create .flowspot directory: " + e.getMessage());
        }
        
        return flowspotDir.resolve(FILTER_FILE_NAME);
    }
    
    /**
     * 加载过滤规则
     */
    public void loadFilterRules() {
        loadFilterRules(null);
    }
    
    /**
     * 基于分析路径加载过滤规则
     * 
     * @param analysisPath 分析路径，如果为null则使用项目根目录
     */
    public void loadFilterRules(@Nullable String analysisPath) {
        filterRules.clear();
        
        // 更新过滤文件路径
        this.filterFilePath = getFilterFilePath(analysisPath);
        
        if (!Files.exists(filterFilePath)) {
            logManager.logDebug("Filter file does not exist: " + filterFilePath);
            return;
        }
        
        try (BufferedReader reader = Files.newBufferedReader(filterFilePath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            int loadedRules = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                try {
                    FlowSpotFilterRule rule = FlowSpotFilterRule.fromString(line);
                    filterRules.add(rule);
                    loadedRules++;
                } catch (Exception e) {
                    logManager.logWarning("Failed to parse filter rule at line " + lineNumber + ": " + line);
                    logManager.logWarning("Error: " + e.getMessage());
                }
            }
            
            logManager.logInfo("Loaded " + loadedRules + " filter rules from " + filterFilePath);
            
        } catch (IOException e) {
            logManager.logError("Failed to load filter rules: " + e.getMessage());
        }
    }
    
    /**
     * 保存过滤规则
     */
    public void saveFilterRules() {
        try {
            // 确保目录存在
            Files.createDirectories(filterFilePath.getParent());
            
            try (BufferedWriter writer = Files.newBufferedWriter(filterFilePath, StandardCharsets.UTF_8)) {
                // 写入文件头
                writer.write(FILTER_FILE_HEADER);
                writer.newLine();
                writer.write(FILTER_FILE_FORMAT);
                writer.newLine();
                writer.write("# Created: " + new Date());
                writer.newLine();
                writer.newLine();
                
                // 按创建时间排序写入规则
                List<FlowSpotFilterRule> sortedRules = filterRules.stream()
                    .sorted(Comparator.comparing(FlowSpotFilterRule::getCreatedTime))
                    .collect(Collectors.toList());
                
                for (FlowSpotFilterRule rule : sortedRules) {
                    writer.write(rule.toString());
                    writer.newLine();
                }
            }
            
            logManager.logInfo("Saved " + filterRules.size() + " filter rules to " + filterFilePath);
            
        } catch (IOException e) {
            logManager.logError("Failed to save filter rules: " + e.getMessage());
        }
    }
    
    /**
     * 添加过滤规则
     */
    public void addFilterRule(@NotNull FlowSpotFilterRule rule) {
        if (filterRules.add(rule)) {
            logManager.logInfo("Added filter rule: " + rule.getDisplayName());
            saveFilterRules();
        } else {
            logManager.logDebug("Filter rule already exists: " + rule.getDisplayName());
        }
    }
    
    /**
     * 移除过滤规则
     */
    public boolean removeFilterRule(@NotNull FlowSpotFilterRule rule) {
        if (filterRules.remove(rule)) {
            logManager.logInfo("Removed filter rule: " + rule.getDisplayName());
            saveFilterRules();
            return true;
        }
        return false;
    }
    
    /**
     * 清空所有过滤规则
     */
    public void clearAllFilterRules() {
        int count = filterRules.size();
        filterRules.clear();
        logManager.logInfo("Cleared " + count + " filter rules");
        saveFilterRules();
    }
    
    /**
     * 获取所有过滤规则
     */
    @NotNull
    public Set<FlowSpotFilterRule> getAllFilterRules() {
        return new HashSet<>(filterRules);
    }
    
    /**
     * 获取过滤规则数量
     */
    public int getFilterRuleCount() {
        return filterRules.size();
    }
    
    /**
     * 检查漏洞是否被过滤
     */
    public boolean isVulnerabilityFiltered(@NotNull FlowSpotVulnerability vulnerability) {
        return filterRules.stream().anyMatch(rule -> rule.matches(vulnerability));
    }
    
    /**
     * 应用过滤规则到漏洞集合
     */
    @NotNull
    public FlowSpotVulnerabilityCollection applyFilters(@NotNull FlowSpotVulnerabilityCollection collection) {
        if (filterRules.isEmpty()) {
            logManager.logDebug("No filter rules to apply");
            return collection;
        }
        
        List<FlowSpotVulnerability> originalVulnerabilities = collection.getVulnerabilities();
        List<FlowSpotVulnerability> filteredVulnerabilities = new ArrayList<>();
        int filteredCount = 0;
        
        for (FlowSpotVulnerability vulnerability : originalVulnerabilities) {
            if (!isVulnerabilityFiltered(vulnerability)) {
                filteredVulnerabilities.add(vulnerability);
            } else {
                filteredCount++;
            }
        }
        
        logManager.logInfo("Applied " + filterRules.size() + " filter rules");
        logManager.logInfo("Filtered out " + filteredCount + " vulnerabilities");
        logManager.logInfo("Remaining vulnerabilities: " + filteredVulnerabilities.size());
        
        // 创建新的过滤后的集合，保持原有的分析基础路径
        FlowSpotVulnerabilityCollection filteredCollection = new FlowSpotVulnerabilityCollection(
            collection.getProjectName(), collection.getAnalysisBasePath());
        filteredVulnerabilities.forEach(filteredCollection::addVulnerability);
        
        return filteredCollection;
    }
    
    /**
     * 根据类名和方法名创建精确匹配过滤规则
     */
    @NotNull
    public FlowSpotFilterRule createExactMatchRule(@NotNull String className, @NotNull String methodName) {
        return createExactMatchRule(className, methodName, null);
    }
    
    /**
     * 根据类名和方法名创建精确匹配过滤规则（带描述）
     */
    @NotNull
    public FlowSpotFilterRule createExactMatchRule(@NotNull String className, 
                                                  @NotNull String methodName, 
                                                  @Nullable String description) {
        return new FlowSpotFilterRule(className, methodName, description, FlowSpotFilterRule.FilterType.EXACT_MATCH);
    }
    
    /**
     * 根据类名创建类匹配过滤规则
     */
    @NotNull
    public FlowSpotFilterRule createClassMatchRule(@NotNull String className) {
        return createClassMatchRule(className, null);
    }
    
    /**
     * 根据类名创建类匹配过滤规则（带描述）
     */
    @NotNull
    public FlowSpotFilterRule createClassMatchRule(@NotNull String className, @Nullable String description) {
        return new FlowSpotFilterRule(className, "*", description, FlowSpotFilterRule.FilterType.CLASS_MATCH);
    }
    
    /**
     * 根据方法名创建方法匹配过滤规则
     */
    @NotNull
    public FlowSpotFilterRule createMethodMatchRule(@NotNull String methodName) {
        return createMethodMatchRule(methodName, null);
    }
    
    /**
     * 根据方法名创建方法匹配过滤规则（带描述）
     */
    @NotNull
    public FlowSpotFilterRule createMethodMatchRule(@NotNull String methodName, @Nullable String description) {
        return new FlowSpotFilterRule("*", methodName, description, FlowSpotFilterRule.FilterType.METHOD_MATCH);
    }
    
    /**
     * 获取过滤统计信息
     */
    @NotNull
    public String getFilterStatistics() {
        Map<FlowSpotFilterRule.FilterType, Long> typeCount = filterRules.stream()
            .collect(Collectors.groupingBy(FlowSpotFilterRule::getFilterType, Collectors.counting()));
        
        StringBuilder stats = new StringBuilder();
        stats.append("Filter Rules Statistics:\n");
        stats.append("Total Rules: ").append(filterRules.size()).append("\n");
        
        for (FlowSpotFilterRule.FilterType type : FlowSpotFilterRule.FilterType.values()) {
            long count = typeCount.getOrDefault(type, 0L);
            stats.append("  ").append(type.name()).append(": ").append(count).append("\n");
        }
        
        return stats.toString();
    }
}
