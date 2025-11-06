/*
 * FlowSpot - Independent security vulnerability detection
 * Copyright (C) 2024 FlowSpot Contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package omni.flowspot.annotations;

/**
 * FlowSpot 独立的源代码行注解类
 * 完全独立于 SpotBugs，不继承任何 SpotBugs 类
 */
public class FlowSpotSourceLineAnnotation {
    
    private final String className;
    private final String sourceFile;
    private final int startLine;
    private final int endLine;
    private final int startBytecode;
    private final int endBytecode;
    private String identifierName;
    private String description;
    private String code;
    private String methodName;
    private String nodeType;
    public FlowSpotSourceLineAnnotation(String className, String sourceFile, int startLine, int endLine, int startBytecode, int endBytecode) {
        this.className = className != null ? className : "Unknown";
        this.sourceFile = sourceFile != null ? sourceFile : "Unknown";
        this.startLine = startLine;
        this.endLine = endLine;
        this.startBytecode = startBytecode;
        this.endBytecode = endBytecode;
    }
    
    /**
     * 获取类名
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * 获取源文件名
     */
    public String getSourceFile() {
        return sourceFile;
    }
    
    /**
     * 获取开始行号
     */
    public int getStartLine() {
        return startLine;
    }
    
    /**
     * 获取结束行号
     */
    public int getEndLine() {
        return endLine;
    }

    public String getNodeType() {
        return nodeType;
    }
    public void setNodeType(String nodeType) {this.nodeType = nodeType;}

    /**
     * 获取开始字节码位置
     */
    public int getStartBytecode() {
        return startBytecode;
    }
    
    /**
     * 获取结束字节码位置
     */
    public int getEndBytecode() {
        return endBytecode;
    }
    
    /**
     * 设置标识符名称
     * FlowSpot 特有功能
     */
    public void setIdentifierName(String identifierName) {
        this.identifierName = identifierName;
    }
    
    /**
     * 获取标识符名称
     * FlowSpot 特有功能
     */
    public String getIdentifierName() {
        return identifierName;
    }
    
    /**
     * 设置描述
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 获取描述
     */
    public String getDescription() {
        return description != null ? description : generateDefaultDescription();
    }
    
    /**
     * 获取代码片段（用于显示）
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 设置代码片段
     */
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * 获取方法名
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * 设置方法名
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    /**
     * 生成默认描述
     */
    private String generateDefaultDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Source line in ").append(className);
        if (sourceFile != null && !sourceFile.equals("Unknown")) {
            sb.append(" (").append(sourceFile).append(")");
        }
        sb.append(" at line ").append(startLine);
        if (startLine != endLine) {
            sb.append("-").append(endLine);
        }
        if (identifierName != null) {
            sb.append(" - ").append(identifierName);
        }
        return sb.toString();
    }
    
    /**
     * 检查是否有有效的行号信息
     */
    public boolean hasLineInfo() {
        return startLine > 0;
    }
    
    /**
     * 检查是否有有效的字节码信息
     */
    public boolean hasBytecodeInfo() {
        return startBytecode >= 0;
    }
    
    @Override
    public String toString() {
        return String.format("FlowSpotSourceLineAnnotation[class=%s, file=%s, line=%d-%d, identifier=%s]", 
                           className, sourceFile, startLine, endLine, identifierName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FlowSpotSourceLineAnnotation that = (FlowSpotSourceLineAnnotation) obj;
        return startLine == that.startLine &&
               endLine == that.endLine &&
               startBytecode == that.startBytecode &&
               endBytecode == that.endBytecode &&
               className.equals(that.className) &&
               sourceFile.equals(that.sourceFile) &&
               java.util.Objects.equals(identifierName, that.identifierName);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(className, sourceFile, startLine, endLine, startBytecode, endBytecode, identifierName);
    }
}
