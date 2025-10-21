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
package com.flowspot.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.flowspot.intellij.model.FlowSpotVulnerabilityCollection;
import com.flowspot.intellij.service.FlowSpotResultsPublisher;
import org.jetbrains.annotations.NotNull;

/**
 * FlowSpot 清理操作
 * 清理分析结果并关闭工具窗口
 */
public class FlowSpotClearAction extends AnAction {
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
            return;
        }
        
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("FlowSpot");
        if (toolWindow == null || !toolWindow.isAvailable()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
            return;
        }
        
        // 工具窗口可用时就启用清理按钮
        boolean isRunning = false; // 简化实现，总是允许清理
        e.getPresentation().setEnabled(!isRunning);
        e.getPresentation().setVisible(true);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("FlowSpot");
        if (toolWindow == null || !toolWindow.isAvailable()) {
            return;
        }
        
        // 清理分析结果
        clearAnalysisResults(project);
        
        // 隐藏工具窗口
        toolWindow.hide(null);
    }
    
    /**
     * 清理分析结果
     */
    private void clearAnalysisResults(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 发布空的结果集合来清理UI（不需要分析路径）
            FlowSpotVulnerabilityCollection emptyCollection = new FlowSpotVulnerabilityCollection(project.getName());
            
            MessageBus messageBus = project.getMessageBus();
            FlowSpotResultsPublisher publisher = messageBus.syncPublisher(FlowSpotResultsPublisher.TOPIC);
            publisher.onFlowSpotResultsAvailable(emptyCollection);
        });
    }
}
