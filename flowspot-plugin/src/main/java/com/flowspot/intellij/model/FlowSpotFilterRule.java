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
package com.flowspot.intellij.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * FlowSpot 数据流过滤规则
 * 用于过滤包含特定方法和类名的漏洞实例
 */
public class FlowSpotFilterRule {
    
    private final String className;
    private final String methodName;
    private final String description;
    private final LocalDateTime createdTime;
    private final FilterType filterType;
    
    /**
     * 过滤类型
     */
    public enum FilterType {
        /**
         * 精确匹配：完全匹配类名和方法名
         */
        EXACT_MATCH,
        
        /**
         * 类名匹配：只匹配类名，忽略方法名
         */
        CLASS_MATCH,
        
        /**
         * 方法名匹配：只匹配方法名，忽略类名
         */
        METHOD_MATCH,
        
        /**
         * 包含匹配：类名或方法名包含指定字符串
         */
        CONTAINS_MATCH
    }
    
    /**
     * 构造函数
     */
    public FlowSpotFilterRule(@NotNull String className, 
                             @NotNull String methodName, 
                             @Nullable String description,
                             @NotNull FilterType filterType) {
        this.className = className;
        this.methodName = methodName;
        this.description = description != null ? description : generateDefaultDescription();
        this.createdTime = LocalDateTime.now();
        this.filterType = filterType;
    }
    
    /**
     * 从字符串解析过滤规则
     * 格式：filterType|className|methodName|description|createdTime
     */
    public static FlowSpotFilterRule fromString(@NotNull String line) {
        String[] parts = line.split("\\|", 5);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid filter rule format: " + line);
        }
        
        FilterType filterType = FilterType.valueOf(parts[0]);
        String className = parts[1];
        String methodName = parts[2];
        String description = parts[3];
        
        FlowSpotFilterRule rule = new FlowSpotFilterRule(className, methodName, description, filterType);
        
        // 如果有时间戳，尝试解析
        if (parts.length == 5) {
            try {
                LocalDateTime createdTime = LocalDateTime.parse(parts[4], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return new FlowSpotFilterRule(className, methodName, description, filterType, createdTime);
            } catch (Exception e) {
                // 忽略时间戳解析错误，使用当前时间
            }
        }
        
        return rule;
    }
    
    /**
     * 带时间戳的构造函数（用于反序列化）
     */
    private FlowSpotFilterRule(@NotNull String className, 
                              @NotNull String methodName, 
                              @Nullable String description,
                              @NotNull FilterType filterType,
                              @NotNull LocalDateTime createdTime) {
        this.className = className;
        this.methodName = methodName;
        this.description = description != null ? description : generateDefaultDescription();
        this.createdTime = createdTime;
        this.filterType = filterType;
    }
    
    /**
     * 转换为字符串格式用于持久化
     */
    public String toString() {
        return String.join("|", 
            filterType.name(),
            className,
            methodName,
            description,
            createdTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
    
    /**
     * 检查漏洞是否匹配此过滤规则
     */
    public boolean matches(@NotNull FlowSpotVulnerability vulnerability) {
        // 检查漏洞的数据流路径中是否包含匹配的节点
        for (FlowSpotLocation location : vulnerability.getLocations()) {
            if (matchesLocation(location)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查位置是否匹配过滤规则
     */
    private boolean matchesLocation(@NotNull FlowSpotLocation location) {
        String locationClassName = location.getClassName();
        String locationMethodName = location.getMethodName();
        
        if (locationClassName == null || locationMethodName == null) {
            return false;
        }
        
        switch (filterType) {
            case EXACT_MATCH:
                return className.equals(locationClassName) && methodName.equals(locationMethodName);
                
            case CLASS_MATCH:
                return className.equals(locationClassName);
                
            case METHOD_MATCH:
                return methodName.equals(locationMethodName);
                
            case CONTAINS_MATCH:
                return locationClassName.contains(className) || locationMethodName.contains(methodName);
                
            default:
                return false;
        }
    }
    
    /**
     * 生成默认描述
     */
    private String generateDefaultDescription() {
        switch (filterType) {
            case EXACT_MATCH:
                return String.format("Filter exact match: %s.%s", className, methodName);
            case CLASS_MATCH:
                return String.format("Filter class: %s", className);
            case METHOD_MATCH:
                return String.format("Filter method: %s", methodName);
            case CONTAINS_MATCH:
                return String.format("Filter contains: %s or %s", className, methodName);
            default:
                return "Custom filter rule";
        }
    }
    
    // Getters
    @NotNull
    public String getClassName() {
        return className;
    }
    
    @NotNull
    public String getMethodName() {
        return methodName;
    }
    
    @NotNull
    public String getDescription() {
        return description;
    }
    
    @NotNull
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    @NotNull
    public FilterType getFilterType() {
        return filterType;
    }
    
    /**
     * 获取显示名称
     */
    @NotNull
    public String getDisplayName() {
        switch (filterType) {
            case EXACT_MATCH:
                return className + "." + methodName;
            case CLASS_MATCH:
                return className + ".*";
            case METHOD_MATCH:
                return "*." + methodName;
            case CONTAINS_MATCH:
                return "*" + className + "* or *" + methodName + "*";
            default:
                return description;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlowSpotFilterRule that = (FlowSpotFilterRule) o;
        return Objects.equals(className, that.className) &&
               Objects.equals(methodName, that.methodName) &&
               filterType == that.filterType;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, filterType);
    }
}
