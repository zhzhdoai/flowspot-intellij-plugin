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
package com.flowspot.intellij.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.flowspot.intellij.gui.FlowSpotDataFlowPanel;
import org.jetbrains.annotations.NotNull;

/**
 * FlowSpot 数据流工具窗口工厂
 * 用于创建右侧数据流可视化面板
 */
public class FlowSpotDataFlowToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建数据流面板
        FlowSpotDataFlowPanel dataFlowPanel = new FlowSpotDataFlowPanel(project);
        
        // 创建内容并添加到工具窗口
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(dataFlowPanel, "", false);
        toolWindow.getContentManager().addContent(content);
        
        // 设置工具窗口属性
        toolWindow.setStripeTitle("Data Flow");
        toolWindow.setTitle("FlowSpot Data Flow");
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
