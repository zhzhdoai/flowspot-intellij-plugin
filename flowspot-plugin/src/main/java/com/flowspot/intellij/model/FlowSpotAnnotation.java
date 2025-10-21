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

/**
 * FlowSpot 注解信息模型
 * 表示漏洞相关的注解信息
 */
public class FlowSpotAnnotation {
    
    public enum AnnotationType {
        SOURCE_LINE,
        ENHANCED_SOURCE_LINE,
        CLASS,
        METHOD,
        FIELD,
        VARIABLE,
        STRING,
        INTEGER,
        OTHER
    }
    
    private final AnnotationType type;
    private final String description;
    private final FlowSpotLocation location;
    private final String pattern;
    private final String value;
    
    public FlowSpotAnnotation(@NotNull AnnotationType type,
                             @NotNull String description,
                             @Nullable FlowSpotLocation location,
                             @Nullable String pattern,
                             @Nullable String value) {
        this.type = type;
        this.description = description;
        this.location = location;
        this.pattern = pattern;
        this.value = value;
    }
    
    // Getters
    @NotNull
    public AnnotationType getType() { return type; }
    
    @NotNull
    public String getDescription() { return description; }
    
    @Nullable
    public FlowSpotLocation getLocation() { return location; }
    
    @Nullable
    public String getPattern() { return pattern; }
    
    @Nullable
    public String getValue() { return value; }
    
    /**
     * 检查是否有位置信息
     */
    public boolean hasLocation() {
        return location != null;
    }
    
    /**
     * 检查是否有模式信息
     */
    public boolean hasPattern() {
        return pattern != null && !pattern.isEmpty();
    }
    
    /**
     * 检查是否有值信息
     */
    public boolean hasValue() {
        return value != null && !value.isEmpty();
    }
    
    /**
     * 获取注解的显示文本
     */
    @NotNull
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(type.name()).append("] ");
        sb.append(description);
        
        if (hasLocation()) {
            sb.append(" at ").append(location.getDisplayText());
        }
        
        if (hasPattern()) {
            sb.append(" (pattern: ").append(pattern).append(")");
        }
        
        if (hasValue()) {
            sb.append(" = ").append(value);
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getDisplayText();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FlowSpotAnnotation that = (FlowSpotAnnotation) obj;
        return type == that.type &&
               description.equals(that.description) &&
               java.util.Objects.equals(location, that.location) &&
               java.util.Objects.equals(pattern, that.pattern) &&
               java.util.Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, description, location, pattern, value);
    }
}
