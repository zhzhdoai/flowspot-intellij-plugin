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
 * FlowSpot 位置信息模型
 * 表示漏洞在代码中的具体位置
 */
public class FlowSpotLocation {
    
    private final String className;
    private final String fileName;
    private final String methodName;
    private final int startLine;
    private final int endLine;
    private final int startColumn;
    private final int endColumn;
    private final String nodeType;
    private final String identifierName;
    private final String description;
    private String code;
    
    public FlowSpotLocation(@NotNull String className,
                           @NotNull String fileName,
                           @Nullable String methodName,
                           int startLine,
                           int endLine,
                           int startColumn,
                           int endColumn,
                           @Nullable String nodeType,
                           @Nullable String identifierName,
                           @Nullable String description) {
        this.className = className;
        this.fileName = fileName;
        this.methodName = methodName;
        this.startLine = startLine;
        this.endLine = endLine;
        this.startColumn = startColumn;
        this.endColumn = endColumn;
        this.nodeType = nodeType;
        this.identifierName = identifierName;
        this.description = description;
    }
    
    // Getters
    @NotNull
    public String getClassName() { return className; }
    
    @NotNull
    public String getFileName() { return fileName; }
    
    @Nullable
    public String getMethodName() { return methodName; }
    
    public int getStartLine() { return startLine; }
    
    public int getEndLine() { return endLine; }
    
    public int getStartColumn() { return startColumn; }
    
    public int getEndColumn() { return endColumn; }
    
    @Nullable
    public String getNodeType() { return nodeType; }
    
    @Nullable
    public String getIdentifierName() { return identifierName; }
    
    @Nullable
    public String getDescription() { return description; }
    
    @Nullable
    public String getCode() { return code; }
    
    public void setCode(@Nullable String code) { this.code = code; }
    
    /**
     * 获取简单的文件名（不包含路径）
     */
    @NotNull
    public String getSimpleFileName() {
        int lastSlash = fileName.lastIndexOf('/');
        int lastBackslash = fileName.lastIndexOf('\\');
        int lastSeparator = Math.max(lastSlash, lastBackslash);
        return lastSeparator >= 0 ? fileName.substring(lastSeparator + 1) : fileName;
    }
    
    /**
     * 获取简单的类名（不包含包名）
     */
    @NotNull
    public String getSimpleClassName() {
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 ? className.substring(lastDot + 1) : className;
    }
    
    /**
     * 检查是否是有效的行号
     */
    public boolean hasValidLineNumber() {
        return startLine > 0;
    }
    
    /**
     * 检查是否有列信息
     */
    public boolean hasColumnInfo() {
        return startColumn >= 0 && endColumn >= 0;
    }
    
    /**
     * 获取位置的显示文本
     */
    @NotNull
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        
        if (methodName != null && !methodName.isEmpty()) {
            sb.append(methodName);
        } else {
            sb.append(getSimpleClassName());
        }
        
        sb.append(" (").append(getSimpleFileName());
        if (hasValidLineNumber()) {
            sb.append(":").append(startLine);
            if (endLine != startLine && endLine > 0) {
                sb.append("-").append(endLine);
            }
        }
        sb.append(")");
        
        if (identifierName != null && !identifierName.isEmpty()) {
            sb.append(" - ").append(identifierName);
        }
        
        return sb.toString();
    }
    
    /**
     * 检查是否有行号信息
     */
    public boolean hasLineInfo() {
        return startLine > 0;
    }
    
    
    @Override
    public String toString() {
        return getDisplayText();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FlowSpotLocation that = (FlowSpotLocation) obj;
        return startLine == that.startLine &&
               endLine == that.endLine &&
               startColumn == that.startColumn &&
               endColumn == that.endColumn &&
               className.equals(that.className) &&
               fileName.equals(that.fileName) &&
               java.util.Objects.equals(methodName, that.methodName);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(className, fileName, methodName, startLine, endLine, startColumn, endColumn);
    }
}
