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

import omni.scan.Analyzer;
import omni.scan.Query;
import omni.scan.generate.SinkQueryGenerator;
import scala.jdk.javaapi.CollectionConverters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * FlowSpot 动态规则加载器
 * 从 FlowSpot 引擎动态加载 sources 和 sinks 规则
 */
public class FlowSpotRuleLoader {
    
    /**
     * Source 规则分类信息
     */
    public static class SourceCategory {
        private final String name;
        private final List<Query> rules;
        
        public SourceCategory(String name, List<Query> rules) {
            this.name = name;
            this.rules = new ArrayList<>(rules);
        }
        
        public String getName() { return name; }
        public List<Query> getRules() { return new ArrayList<>(rules); }
        public int getCount() { return rules.size(); }
    }
    
    /**
     * Sink 规则分类信息
     */
    public static class SinkCategory {
        private final String category;
        private final Map<String, List<Query>> subCategories;
        
        public SinkCategory(String category) {
            this.category = category;
            this.subCategories = new LinkedHashMap<>();
        }
        
        public void addRule(String queryName, Query rule) {
            subCategories.computeIfAbsent(queryName, k -> new ArrayList<>()).add(rule);
        }
        
        public String getCategory() { return category; }
        public Map<String, List<Query>> getSubCategories() { return new LinkedHashMap<>(subCategories); }
        public int getTotalCount() { 
            return subCategories.size(); // 返回unique name的数量
        }
    }
    
    /**
     * 动态加载 Source 规则并按文件名分类
     */
    @NotNull
    public static Map<String, SourceCategory> loadSourceCategories() {
        Map<String, SourceCategory> categories = new LinkedHashMap<>();
        
        try {
            // 创建 Analyzer 实例并获取 source 规则
            Analyzer analyzer = new Analyzer();
            List<Query> sourceQueries = CollectionConverters.asJava(analyzer.getSourcesQuery());
            
            // 按文件名进行分类
            Map<String, List<Query>> categoryMap = new HashMap<>();
            
            for (Query query : sourceQueries) {
                String categoryName = extractSourceCategory(query);
                categoryMap.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(query);
            }
            
            // 转换为 SourceCategory 对象
            for (Map.Entry<String, List<Query>> entry : categoryMap.entrySet()) {
                categories.put(entry.getKey(), new SourceCategory(entry.getKey(), entry.getValue()));
            }
            
        } catch (Exception e) {
            // 如果加载失败，创建一个空的分类
            categories.put("Unknown Sources", new SourceCategory("Unknown Sources", new ArrayList<>()));
        }
        
        return categories;
    }
    
    /**
     * 动态加载 Sink 规则并按 rule.category 分类
     */
    @NotNull
    public static Map<String, SinkCategory> loadSinkCategories() {
        return loadSinkCategories(null);
    }
    
    /**
     * 动态加载 Sink 规则并按 rule.category 分类
     * @param projectSinksJsonPath 项目特定的sinks.json路径，如果为null则使用默认路径
     */
    @NotNull
    public static Map<String, SinkCategory> loadSinkCategories(@Nullable String projectSinksJsonPath) {
        Map<String, SinkCategory> categories = new LinkedHashMap<>();
        
        try {
            // 1. 创建 Analyzer 实例并获取所有 sink 规则（包括内置规则和JSON规则）
            Analyzer analyzer = new Analyzer();
            List<Query> allSinkQueries;
            
            if (projectSinksJsonPath != null && !projectSinksJsonPath.isEmpty()) {
                // 使用项目特定的sinks.json，这会包含JSON规则和内置规则
                allSinkQueries = CollectionConverters.asJava(analyzer.getSinksQuery(projectSinksJsonPath));
            } else {
                // 使用默认的sinks.json，这会包含JSON规则和内置规则
                allSinkQueries = CollectionConverters.asJava(analyzer.getSinksQuery());
            }
            
            // 按 rule.category 进行分类
            for (Query query : allSinkQueries) {
                String categoryName = extractSinkCategory(query);
                String queryName = extractQueryName(query);
                
                SinkCategory category = categories.computeIfAbsent(categoryName, SinkCategory::new);
                category.addRule(queryName, query);
            }
            
            
        } catch (Exception e) {
            // 如果加载失败，创建一个空的分类
            SinkCategory unknownCategory = new SinkCategory("Unknown Sinks");
            categories.put("Unknown Sinks", unknownCategory);
        }
        
        return categories;
    }
    
    /**
     * 从 Query 中提取 Source 分类名称
     * 基于文件名进行自动分类
     */
    @NotNull
    private static String extractSourceCategory(@NotNull Query query) {
        try {
            // 获取查询名称，通常包含文件信息
            String queryName = query.name();
            
            // 基于文件名模式进行分类
            if (queryName.toLowerCase().contains("http") || queryName.toLowerCase().contains("servlet")) {
                return "HTTP Sources";
            } else if (queryName.toLowerCase().contains("spring") || queryName.toLowerCase().contains("mvc")) {
                return "Spring Sources";
            } else if (queryName.toLowerCase().contains("web") || queryName.toLowerCase().contains("request")) {
                return "Web Sources";
            } else if (queryName.toLowerCase().contains("file") || queryName.toLowerCase().contains("io")) {
                return "File I/O Sources";
            } else if (queryName.toLowerCase().contains("system") || queryName.toLowerCase().contains("env")) {
                return "System Sources";
            } else if (queryName.toLowerCase().contains("database") || queryName.toLowerCase().contains("jdbc")) {
                return "Database Sources";
            } else if (queryName.toLowerCase().contains("network") || queryName.toLowerCase().contains("socket")) {
                return "Network Sources";
            } else {
                return "Other Sources";
            }
        } catch (Exception e) {
            return "Unknown Sources";
        }
    }
    
    /**
     * 从 Query 中提取 Sink 分类名称
     * 基于 rule.category 字段和查询描述
     */
    @NotNull
    private static String extractSinkCategory(@NotNull Query query) {
        try {
            return query.category();
        } catch (Exception e) {
            return "未知漏洞分类";
        }
    }
    
    /**
     * 从 Query 中提取查询名称
     * 移除数字后缀，例如 "SQL_INJECTION_TAGGER_0" -> "SQL_INJECTION_TAGGER"
     */
    @NotNull
    private static String extractQueryName(@NotNull Query query) {
        try {
            String queryName = query.name();
            // 移除末尾的 "_数字" 后缀
            return queryName.replaceAll("_\\d+$", "");
        } catch (Exception e) {
            return "Unknown Query";
        }
    }
    
    /**
     * 获取选中的 Source 规则
     */
    @NotNull
    public static Set<String> getSelectedSourceRules(@NotNull Set<String> selectedItems, 
                                                    @NotNull Map<String, SourceCategory> categories) {
        Set<String> selectedRules = new HashSet<>();
        
        for (String item : selectedItems) {
            // 检查是否是分类名称
            if (categories.containsKey(item)) {
                // 添加该分类下的所有规则
                SourceCategory category = categories.get(item);
                for (Query rule : category.getRules()) {
                    selectedRules.add(rule.name());
                }
            } else {
                // 检查是否是具体的规则名称
                for (SourceCategory category : categories.values()) {
                    for (Query rule : category.getRules()) {
                        if (rule.name().equals(item)) {
                            selectedRules.add(item);
                            break;
                        }
                    }
                }
            }
        }
        
        return selectedRules;
    }
    
    /**
     * 获取选中的 Sink 规则
     */
    @NotNull
    public static Set<String> getSelectedSinkRules(@NotNull Set<String> selectedItems, 
                                                  @NotNull Map<String, SinkCategory> categories) {
        Set<String> selectedRules = new HashSet<>();
        
        for (String item : selectedItems) {
            
            // 首先检查是否是具体的规则名称
            boolean foundAsRule = false;
            for (SinkCategory category : categories.values()) {
                for (Map.Entry<String, List<Query>> subEntry : category.getSubCategories().entrySet()) {
                    if (subEntry.getKey().equals(item)) {
                        // 直接添加子分类名称（已经是去掉数字后缀的）
                        selectedRules.add(item);
                        foundAsRule = true;
                        break;
                    }
                }
                if (foundAsRule) break;
            }
            
            // 如果不是具体规则，再检查是否是分类名称
            if (!foundAsRule && categories.containsKey(item)) {
                // 添加该分类下的所有规则（去掉数字后缀）
                SinkCategory category = categories.get(item);
                for (String subCategoryName : category.getSubCategories().keySet()) {
                    selectedRules.add(subCategoryName); // 使用去掉数字后缀的名称
                }
            } else if (!foundAsRule) {
                // 如果没找到，直接添加（可能是内置规则）
                selectedRules.add(item);
            }
        }
        
        return selectedRules;
    }
}
