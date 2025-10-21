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

import com.intellij.util.messages.Topic;
import com.flowspot.intellij.model.FlowSpotVulnerabilityCollection;
import org.jetbrains.annotations.NotNull;

/**
 * FlowSpot 结果发布接口
 * 用于通过消息总线发布分析结果
 */
public interface FlowSpotResultsPublisher {
    
    /**
     * 消息总线主题
     */
    Topic<FlowSpotResultsPublisher> TOPIC = Topic.create("FlowSpot Results", FlowSpotResultsPublisher.class);
    
    /**
     * 当 FlowSpot 分析结果可用时调用
     * 
     * @param collection 漏洞结果集合
     */
    void onFlowSpotResultsAvailable(@NotNull FlowSpotVulnerabilityCollection collection);
    
    /**
     * 当分析开始时调用
     * 
     * @param projectName 项目名称
     */
    default void onAnalysisStarted(@NotNull String projectName) {
        // 默认实现为空
    }
    
    /**
     * 当分析完成时调用
     * 
     * @param projectName 项目名称
     * @param success 是否成功
     * @param errorMessage 错误消息（如果失败）
     */
    default void onAnalysisCompleted(@NotNull String projectName, boolean success, @NotNull String errorMessage) {
        // 默认实现为空
    }
    
    /**
     * 当分析被取消时调用
     * 
     * @param projectName 项目名称
     */
    default void onAnalysisCancelled(@NotNull String projectName) {
        // 默认实现为空
    }
    
    /**
     * 当分析进度更新时调用
     * 
     * @param projectName 项目名称
     * @param progress 进度（0.0 - 1.0）
     * @param message 进度消息
     */
    default void onAnalysisProgress(@NotNull String projectName, double progress, @NotNull String message) {
        // 默认实现为空
    }
}
