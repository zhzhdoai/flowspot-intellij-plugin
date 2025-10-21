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
package com.flowspot.intellij.gui;

import com.flowspot.intellij.model.FlowSpotVulnerability;
import com.flowspot.intellij.model.FlowSpotLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * FlowSpot 树节点数据封装类
 * 用于在漏洞树中存储节点相关的数据
 */
public class FlowSpotTreeNodeData {
    
    private final String displayText;
    private final FlowSpotVulnerability vulnerability;
    private final FlowSpotLocation location;
    private final NodeType nodeType;
    private final int count;
    
    /**
     * 节点类型枚举
     */
    public enum NodeType {
        ROOT,           // 根节点
        CATEGORY,       // 分类节点
        VULNERABILITY,  // 漏洞节点
        DATA_FLOW,      // 数据流节点
        LOCATION        // 位置节点
    }
    
    /**
     * 创建根节点数据
     */
    public static FlowSpotTreeNodeData createRootNode(@NotNull String displayText, int count) {
        return new FlowSpotTreeNodeData(displayText, null, null, NodeType.ROOT, count);
    }
    
    /**
     * 创建分类节点数据
     */
    public static FlowSpotTreeNodeData createCategoryNode(@NotNull String displayText, int count) {
        return new FlowSpotTreeNodeData(displayText, null, null, NodeType.CATEGORY, count);
    }
    
    /**
     * 创建漏洞节点数据
     */
    public static FlowSpotTreeNodeData createVulnerabilityNode(@NotNull FlowSpotVulnerability vulnerability) {
        String displayText = vulnerability.getTitle() + " (" + vulnerability.getSeverity() + ")";
        return new FlowSpotTreeNodeData(displayText, vulnerability, null, NodeType.VULNERABILITY, 1);
    }
    
    /**
     * 创建数据流节点数据
     */
    public static FlowSpotTreeNodeData createDataFlowNode(@NotNull String displayText, 
                                                         @NotNull FlowSpotVulnerability vulnerability) {
        return new FlowSpotTreeNodeData(displayText, vulnerability, null, NodeType.DATA_FLOW, 0);
    }
    
    /**
     * 创建位置节点数据
     */
    public static FlowSpotTreeNodeData createLocationNode(@NotNull FlowSpotLocation location, 
                                                        @NotNull FlowSpotVulnerability vulnerability) {
        String displayText = formatLocationText(location);
        return new FlowSpotTreeNodeData(displayText, vulnerability, location, NodeType.LOCATION, 0);
    }
    
    /**
     * 私有构造函数
     */
    private FlowSpotTreeNodeData(@NotNull String displayText, 
                               @Nullable FlowSpotVulnerability vulnerability,
                               @Nullable FlowSpotLocation location,
                               @NotNull NodeType nodeType,
                               int count) {
        this.displayText = displayText;
        this.vulnerability = vulnerability;
        this.location = location;
        this.nodeType = nodeType;
        this.count = count;
    }
    
    /**
     * 格式化位置文本
     */
    private static String formatLocationText(@NotNull FlowSpotLocation location) {
        StringBuilder sb = new StringBuilder();
        
        if (location.getClassName() != null && !location.getClassName().isEmpty()) {
            // 简化类名显示
            String className = location.getClassName();
            int lastDot = className.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < className.length() - 1) {
                className = className.substring(lastDot + 1);
            }
            sb.append(className);
        }
        
        if (location.getMethodName() != null && !location.getMethodName().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(location.getMethodName()).append("()");
        }
        
        if (location.getStartLine() > 0) {
            sb.append(" (line ").append(location.getStartLine()).append(")");
        }
        
        if (sb.length() == 0) {
            sb.append("Unknown location");
        }
        
        return sb.toString();
    }
    
    // Getters
    
    @NotNull
    public String getDisplayText() {
        return displayText;
    }
    
    @Nullable
    public FlowSpotVulnerability getVulnerability() {
        return vulnerability;
    }
    
    @Nullable
    public FlowSpotLocation getLocation() {
        return location;
    }
    
    @NotNull
    public NodeType getNodeType() {
        return nodeType;
    }
    
    public int getCount() {
        return count;
    }
    
    /**
     * 检查是否为叶子节点
     */
    public boolean isLeaf() {
        return nodeType == NodeType.VULNERABILITY || nodeType == NodeType.LOCATION;
    }
    
    /**
     * 检查是否可以导航到源代码
     */
    public boolean isNavigable() {
        return location != null && location.getFileName() != null && location.getStartLine() > 0;
    }
    
    @Override
    public String toString() {
        return displayText;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FlowSpotTreeNodeData that = (FlowSpotTreeNodeData) obj;
        return displayText.equals(that.displayText) && 
               nodeType == that.nodeType &&
               Objects.equals(vulnerability, that.vulnerability) &&
               Objects.equals(location, that.location);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(displayText, nodeType, vulnerability, location);
    }
}
