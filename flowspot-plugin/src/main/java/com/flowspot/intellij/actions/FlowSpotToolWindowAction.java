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
import org.jetbrains.annotations.NotNull;

/**
 * FlowSpot 工具窗口切换操作
 * 用于在 IDEA 右侧显示 FlowSpot 按钮，点击后显示/隐藏 FlowSpot 工具窗口
 */
public class FlowSpotToolWindowAction extends AnAction {
    
    public FlowSpotToolWindowAction() {
        super("FlowSpot", "Show/Hide FlowSpot Tool Window", null);
    }
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("FlowSpot");
        
        if (toolWindow != null) {
            if (toolWindow.isVisible()) {
                toolWindow.hide();
            } else {
                toolWindow.show();
                toolWindow.activate(null);
            }
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
        
        if (project != null) {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow("FlowSpot");
            
            if (toolWindow != null && toolWindow.isVisible()) {
                e.getPresentation().setText("Hide FlowSpot");
                e.getPresentation().setDescription("Hide FlowSpot Tool Window");
            } else {
                e.getPresentation().setText("Show FlowSpot");
                e.getPresentation().setDescription("Show FlowSpot Tool Window");
            }
        }
    }
}
