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

/**
 * FlowSpot 优化配置类
 * 用于配置数据流分析的优化选项
 */
public class OptimizationConfig {
    
    private final boolean enableSubPathDeduplication;
    private final boolean enableSinkLocationDeduplication;
    private final boolean enableContextFiltering;
    
    /**
     * 构造函数
     * 
     * @param enableSubPathDeduplication 是否启用子路径去重
     * @param enableSinkLocationDeduplication 是否启用Sink位置去重
     * @param enableContextFiltering 是否启用上下文过滤
     */
    public OptimizationConfig(boolean enableSubPathDeduplication,
                            boolean enableSinkLocationDeduplication,
                            boolean enableContextFiltering) {
        this.enableSubPathDeduplication = enableSubPathDeduplication;
        this.enableSinkLocationDeduplication = enableSinkLocationDeduplication;
        this.enableContextFiltering = enableContextFiltering;
    }
    
    /**
     * 创建默认配置（所有优化都启用）
     */
    public static OptimizationConfig createDefault() {
        return new OptimizationConfig(true, true, true);
    }
    
    /**
     * 是否启用子路径去重
     */
    public boolean isEnableSubPathDeduplication() {
        return enableSubPathDeduplication;
    }
    
    /**
     * 是否启用Sink位置去重
     */
    public boolean isEnableSinkLocationDeduplication() {
        return enableSinkLocationDeduplication;
    }
    
    /**
     * 是否启用上下文过滤
     */
    public boolean isEnableContextFiltering() {
        return enableContextFiltering;
    }
    
    /**
     * 转换为Scala项目中的OptimizationConfig
     * 这个方法将在传递给Scala项目时使用
     */
    public omni.scan.OptimizationConfig toScalaConfig() {
        return new omni.scan.OptimizationConfig(
            enableSubPathDeduplication,
            enableSinkLocationDeduplication,
            enableContextFiltering
        );
    }
    
    @Override
    public String toString() {
        return "OptimizationConfig{" +
                "enableSubPathDeduplication=" + enableSubPathDeduplication +
                ", enableSinkLocationDeduplication=" + enableSinkLocationDeduplication +
                ", enableContextFiltering=" + enableContextFiltering +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        OptimizationConfig that = (OptimizationConfig) o;
        
        if (enableSubPathDeduplication != that.enableSubPathDeduplication) return false;
        if (enableSinkLocationDeduplication != that.enableSinkLocationDeduplication) return false;
        return enableContextFiltering == that.enableContextFiltering;
    }
    
    @Override
    public int hashCode() {
        int result = (enableSubPathDeduplication ? 1 : 0);
        result = 31 * result + (enableSinkLocationDeduplication ? 1 : 0);
        result = 31 * result + (enableContextFiltering ? 1 : 0);
        return result;
    }
}
