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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.flowspot.intellij.core.FlowSpotAnalysisEngine;
import org.jetbrains.annotations.NotNull;

/**
 * FlowSpot 停止分析操作
 * 停止当前正在运行的 FlowSpot 分析
 */
public class FlowSpotStopAction extends AnAction {
    
    private static FlowSpotAnalysisEngine currentEngine;
    
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
        
        // 只有在分析运行时才启用停止按钮
        boolean isRunning = currentEngine != null && currentEngine.isRunning();
        e.getPresentation().setEnabled(isRunning);
        e.getPresentation().setVisible(true);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (currentEngine != null && currentEngine.isRunning()) {
            currentEngine.cancel();
        }
    }
    
    /**
     * 设置当前运行的分析引擎
     * 用于跟踪分析状态
     */
    public static void setCurrentEngine(FlowSpotAnalysisEngine engine) {
        currentEngine = engine;
    }
    
    /**
     * 清除当前分析引擎引用
     */
    public static void clearCurrentEngine() {
        currentEngine = null;
    }
}
