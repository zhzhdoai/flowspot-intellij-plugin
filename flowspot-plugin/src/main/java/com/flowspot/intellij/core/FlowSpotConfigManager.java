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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * FlowSpot 配置文件管理器
 * 负责管理项目特定的配置文件，如sinks.json
 */
public class FlowSpotConfigManager {
    
    private final Project project;
    private final FlowSpotLogManager logManager;
    
    // 插件资源中的默认配置文件路径
    private static final String RESOURCE_SINKS_JSON = "/config/sinks.json";
    private static final String PROJECT_CONFIG_DIR = ".flowspot/config";
    private static final String PROJECT_SINKS_JSON = "sinks.json";
    
    public FlowSpotConfigManager(@NotNull Project project) {
        this.project = project;
        this.logManager = FlowSpotLogManager.getInstance(project);
    }
    


    
    /**
     * 从插件资源复制默认的sinks.json文件
     * 
     * @param targetPath 目标文件路径
     * @return 是否成功复制
     */
    private boolean copyDefaultSinksJsonFromResource(@NotNull Path targetPath) {
        try (InputStream resourceStream = getClass().getResourceAsStream(RESOURCE_SINKS_JSON)) {
            if (resourceStream == null) {
                logManager.logError("Default sinks.json resource not found: " + RESOURCE_SINKS_JSON, null);
                return false;
            }
            
            Files.copy(resourceStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
            
        } catch (IOException e) {
            logManager.logError("Failed to copy sinks.json from resources", e);
            return false;
        }
    }
    
    
    /**
     * 获取项目特定的sinks.json文件路径
     * 
     * @return 项目sinks.json路径，如果不存在返回null
     */
    @Nullable
    public String getProjectSinksJsonPath() {
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            return null;
        }
        
        Path projectSinksJson = Paths.get(projectBasePath, PROJECT_CONFIG_DIR, PROJECT_SINKS_JSON);
        return Files.exists(projectSinksJson) ? projectSinksJson.toString() : null;
    }
    
    /**
     * 获取有效的sinks.json文件路径
     * 优先使用项目特定的配置，如果不存在则准备一个
     * 
     * @return 有效的sinks.json路径
     */
    @NotNull
    public String getEffectiveSinksJsonPath() {
        return getEffectiveSinksJsonPath(null);
    }
    
    /**
     * 获取有效的sinks.json文件路径
     * 策略：默认规则 + 全局自定义规则（合并）
     * 
     * @param analysisPath 分析路径（忽略）
     * @return 有效的sinks.json路径
     */
    @NotNull
    public String getEffectiveSinksJsonPath(@Nullable String analysisPath) {
        try {
            // 获取默认规则路径
            String defaultSinksPath = getDefaultSinksJsonPath();
            
            // 使用全局配置服务
            com.flowspot.intellij.service.GlobalSinkRulesService service = 
                com.flowspot.intellij.service.GlobalSinkRulesService.getInstance();
            
            int globalRulesCount = service.getRules().size();
            
            // 如果没有自定义全局规则，直接使用默认规则
            if (globalRulesCount == 0) {
                logManager.logInfo("No custom global rules, using default sinks.json: " + defaultSinksPath);
                return defaultSinksPath;
            }
            
            // 合并默认规则和全局规则
            String mergedJson = mergeDefaultAndGlobalRules(defaultSinksPath, service);
            
            // 创建临时文件存储合并后的规则
            Path tempFile = Files.createTempFile("flowspot-merged-sinks-", ".json");
            Files.write(tempFile, mergedJson.getBytes());
            
            String tempPath = tempFile.toString();
            logManager.logInfo("Using merged sink rules (default + " + globalRulesCount + " custom) from: " + tempPath);
            
            return tempPath;
        } catch (Exception e) {
            logManager.logError("Failed to merge rules, using default", e);
            return getDefaultSinksJsonPath();
        }
    }
    
    /**
     * 合并默认规则和全局自定义规则
     */
    @NotNull
    private String mergeDefaultAndGlobalRules(@NotNull String defaultSinksPath, 
                                              @NotNull com.flowspot.intellij.service.GlobalSinkRulesService service) {
        try {
            // 读取默认规则
            String defaultJson = new String(Files.readAllBytes(Paths.get(defaultSinksPath)));
            
            // 获取全局规则
            String globalJson = service.getRulesAsJson();
            
            // 简单合并：解析两个 JSON 数组，合并后重新序列化
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.reflect.TypeToken<java.util.List<com.flowspot.intellij.model.SinkRule>> typeToken = 
                new com.google.gson.reflect.TypeToken<java.util.List<com.flowspot.intellij.model.SinkRule>>(){};
            
            java.util.List<com.flowspot.intellij.model.SinkRule> defaultRules = 
                gson.fromJson(defaultJson, typeToken.getType());
            java.util.List<com.flowspot.intellij.model.SinkRule> globalRules = 
                gson.fromJson(globalJson, typeToken.getType());
            
            if (defaultRules == null) defaultRules = new java.util.ArrayList<>();
            if (globalRules == null) globalRules = new java.util.ArrayList<>();
            
            // 合并：默认规则 + 全局规则（全局规则优先，可以覆盖同名的默认规则）
            java.util.Map<String, com.flowspot.intellij.model.SinkRule> mergedMap = new java.util.LinkedHashMap<>();
            
            // 先添加默认规则
            for (com.flowspot.intellij.model.SinkRule rule : defaultRules) {
                mergedMap.put(rule.getName(), rule);
            }
            
            // 再添加全局规则（会覆盖同名的默认规则）
            for (com.flowspot.intellij.model.SinkRule rule : globalRules) {
                mergedMap.put(rule.getName(), rule);
            }
            
            java.util.List<com.flowspot.intellij.model.SinkRule> mergedRules = 
                new java.util.ArrayList<>(mergedMap.values());
            
            logManager.logInfo("Merged rules: " + defaultRules.size() + " default + " + 
                             globalRules.size() + " custom = " + mergedRules.size() + " total");
            
            return gson.toJson(mergedRules);
            
        } catch (Exception e) {
            logManager.logError("Failed to merge rules, using global rules only", e);
            return service.getRulesAsJson();
        }
    }
    
    /**
     * 获取默认的 sinks.json 路径（项目配置或插件资源）
     */
    @NotNull
    private String getDefaultSinksJsonPath() {
        // 优先使用项目根目录的配置
        String basePath = project.getBasePath();
        if (basePath != null) {
            String projectBasedPath = getProjectSinksJsonPath();
            if (projectBasedPath != null) {
                logManager.logInfo("Using project-based sinks.json: " + projectBasedPath);
                return projectBasedPath;
            }
            
            // 如果项目配置不存在，准备一个新的
            String preparedPath = prepareAnalysisBasedSinksJson(basePath);
            if (preparedPath != null) {
                logManager.logInfo("Prepared default sinks.json: " + preparedPath);
                return preparedPath;
            }
            
            return Paths.get(basePath, PROJECT_CONFIG_DIR, PROJECT_SINKS_JSON).toString();
        }
        
        return "config/sinks.json";
    }
    
    /**
     * 获取基于分析路径的sinks.json文件路径
     * 
     * @param basePath 基础路径
     * @return sinks.json路径，如果不存在返回null
     */
    @Nullable
    private String getAnalysisBasedSinksJsonPath(@NotNull String basePath) {
        Path sinksJsonPath = Paths.get(basePath, PROJECT_CONFIG_DIR, PROJECT_SINKS_JSON);
        return Files.exists(sinksJsonPath) ? sinksJsonPath.toString() : null;
    }
    
    /**
     * 在指定路径下准备sinks.json配置文件
     * 
     * @param basePath 基础路径
     * @return 准备好的sinks.json路径，如果失败返回null
     */
    @Nullable
    private String prepareAnalysisBasedSinksJson(@NotNull String basePath) {
        try {
            // 创建配置目录
            Path configDir = Paths.get(basePath, PROJECT_CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                logManager.logInfo("Created analysis-based config directory: " + configDir);
            }
            
            // 目标sinks.json路径
            Path sinksJsonPath = configDir.resolve(PROJECT_SINKS_JSON);
            
            // 从全局配置复制 sinks.json（每次都更新，确保使用最新的全局规则）
            if (copyGlobalSinksJsonToProject(sinksJsonPath)) {
                logManager.logInfo("Copied global sinks.json to project: " + sinksJsonPath);
                return sinksJsonPath.toString();
            } else {
                logManager.logError("Failed to copy global sinks.json to project", null);
                return null;
            }
            
        } catch (IOException e) {
            logManager.logError("Failed to prepare analysis-based sinks.json", e);
            return null;
        }
    }
    
    /**
     * 从全局配置复制 sinks.json 到项目目录
     */
    private boolean copyGlobalSinksJsonToProject(@NotNull Path targetPath) {
        try {
            // 获取全局 sinks.json 内容
            com.flowspot.intellij.service.GlobalSinkRulesService service = 
                com.flowspot.intellij.service.GlobalSinkRulesService.getInstance();
            
            String globalJson = service.getRulesAsJson();
            
            // 写入到项目目录
            Files.writeString(targetPath, globalJson);
            
            logManager.logInfo("Copied global sinks rules (" + service.getRules().size() + " rules) to: " + targetPath);
            return true;
            
        } catch (Exception e) {
            logManager.logError("Failed to copy global sinks.json: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 创建默认的sinks.json配置文件
     * 
     * @param targetPath 目标文件路径
     * @return 是否成功创建
     */
    public boolean createDefaultSinksJson(@NotNull String targetPath) {
        try {
            Path target = Paths.get(targetPath);
            
            // 确保父目录存在
            Path parentDir = target.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 从插件资源复制默认配置
            if (copyDefaultSinksJsonFromResource(target)) {
                logManager.logInfo("创建默认 sinks.json: " + targetPath);
                return true;
            } else {
                // 如果复制失败，创建一个简单的默认配置
                String defaultContent = "{\n  \"sinks\": []\n}";
                Files.write(target, defaultContent.getBytes());
                logManager.logInfo("创建简单默认 sinks.json: " + targetPath);
                return true;
            }
            
        } catch (IOException e) {
            logManager.logError("创建默认 sinks.json 失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 检查项目是否有自定义的sinks.json配置
     * 
     * @return true如果存在项目特定的配置
     */
    public boolean hasProjectSpecificConfig() {
        return getProjectSinksJsonPath() != null;
    }
    
    /**
     * 清理资源
     */
    public void dispose() {
        if (logManager != null) {
            logManager.close();
        }
    }
}
