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
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.flowspot.intellij.core.FlowSpotAnalysisEngine;
import com.flowspot.intellij.core.FlowSpotLogManager;
import com.flowspot.intellij.gui.FlowSpotRuleSelectionDialog;
import com.flowspot.intellij.model.FlowSpotVulnerabilityCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 分析项目文件操作
 * 对整个项目执行 FlowSpot 安全分析
 */
public class AnalyzeProjectFilesAction extends AnAction {
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null && !project.isDisposed());
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        
        // 显示规则选择对话框
        FlowSpotRuleSelectionDialog dialog = new FlowSpotRuleSelectionDialog(project);
        if (!dialog.showAndGet()) {
            // 用户取消了对话框
            return;
        }
        
        // 获取用户选择的规则和优化配置
        Set<String> selectedSourceRules = dialog.getSelectedSourceRules();
        Set<String> selectedSinkRules = dialog.getSelectedSinkRules();
        com.flowspot.intellij.model.OptimizationConfig optimizationConfig = dialog.getOptimizationConfig();
        
        // 添加日志记录
        FlowSpotLogManager logManager = FlowSpotLogManager.getInstance(project);
        logManager.logInfo("=== 分析操作中获取到的规则 ===");
        logManager.logInfo("统一日志文件位置: " + System.getProperty("user.home") + "/.flowspot/flowspot-analysis.log");
        logManager.logInfo("Source规则数量: " + selectedSourceRules.size());
        logManager.logInfo("Source规则: " + selectedSourceRules);
        logManager.logInfo("Sink规则数量: " + selectedSinkRules.size());
        logManager.logInfo("Sink规则: " + selectedSinkRules);
        logManager.logInfo("是否有选中的规则: " + dialog.hasSelectedRules());
        
        if (!dialog.hasSelectedRules()) {
            // 没有选择任何规则，提示用户
            logManager.logWarning("没有选中任何规则，取消分析");
            return;
        }
        
        // 显示 FlowSpot 工具窗口
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("FlowSpot");
        if (toolWindow != null) {
            toolWindow.show();
        }
        
        // 在后台任务中执行分析
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "FlowSpot Analysis", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Preparing FlowSpot analysis...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.0);
                    
                    // 收集项目源码路径和类路径
                    List<String> sourcePaths = collectSourcePaths(project, e);
                    List<String> classPaths = collectClassPaths(project);
                    
                    if (sourcePaths.isEmpty()) {
                        indicator.setText("No source files found in project");
                        return;
                    }
                    
                    indicator.setText("Starting FlowSpot vulnerability analysis...");
                    indicator.setText2(dialog.getSelectionSummary());
                    indicator.setFraction(0.1);
                    
                    // 创建分析引擎并执行分析
                    FlowSpotAnalysisEngine engine = new FlowSpotAnalysisEngine(project);
                    
                    // 使用用户选择的规则和优化配置
                    FlowSpotVulnerabilityCollection results = engine.analyze(
                        sourcePaths, classPaths, selectedSourceRules, selectedSinkRules, optimizationConfig
                    );
                    
                    indicator.setText("Analysis completed");
                    indicator.setFraction(1.0);
                    
                } catch (Exception ex) {
                    // 错误处理
                    indicator.setText("Analysis failed: " + ex.getMessage());
                }
            }
        });
    }
    
    /**
     * 收集项目源码路径
     * 优先使用用户右键选中的目录，如果没有选中则使用项目源码根目录
     */
    @NotNull
    private List<String> collectSourcePaths(@NotNull Project project, @NotNull AnActionEvent event) {
        List<String> sourcePaths = new ArrayList<>();
        
        // 首先尝试获取用户选中的文件/目录
        VirtualFile[] selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (selectedFiles != null && selectedFiles.length > 0) {
            FlowSpotLogManager logManager = FlowSpotLogManager.getInstance(project);
            logManager.logInfo("检测到用户选中的文件/目录: " + selectedFiles.length + " 个");
            
            for (VirtualFile selectedFile : selectedFiles) {
                if (selectedFile.exists()) {
                    if (selectedFile.isDirectory()) {
                        // 选中的是目录，直接添加
                        sourcePaths.add(selectedFile.getPath());
                        logManager.logInfo("添加选中的目录: " + selectedFile.getPath());
                    } else {
                        // 选中的是文件，添加其父目录
                        VirtualFile parentDir = selectedFile.getParent();
                        if (parentDir != null && parentDir.exists()) {
                            sourcePaths.add(parentDir.getPath());
                            logManager.logInfo("添加选中文件的父目录: " + parentDir.getPath());
                        }
                    }
                }
            }
        }
        
        // 如果没有选中任何文件，或者选中的文件无效，则使用项目源码根目录
        if (sourcePaths.isEmpty()) {
            FlowSpotLogManager logManager = FlowSpotLogManager.getInstance(project);
            logManager.logInfo("没有检测到有效的选中目录，使用项目源码根目录");
            
            VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
            for (VirtualFile sourceRoot : sourceRoots) {
                if (sourceRoot.exists() && sourceRoot.isDirectory()) {
                    sourcePaths.add(sourceRoot.getPath());
                    logManager.logInfo("添加项目源码根目录: " + sourceRoot.getPath());
                }
            }
            
            // 如果没有找到源码根目录，使用项目根目录
            if (sourcePaths.isEmpty() && project.getBasePath() != null) {
                sourcePaths.add(project.getBasePath());
                logManager.logInfo("使用项目根目录: " + project.getBasePath());
            }
        }
        
        return sourcePaths;
    }
    
    /**
     * 收集项目类路径
     */
    @NotNull
    private List<String> collectClassPaths(@NotNull Project project) {
        List<String> classPaths = new ArrayList<>();
        
        // 添加项目输出目录
        VirtualFile[] outputRoots = ProjectRootManager.getInstance(project).getContentRoots();
        for (VirtualFile outputRoot : outputRoots) {
            if (outputRoot.exists()) {
                // 查找 build/classes 或 target/classes 目录
                VirtualFile buildClasses = outputRoot.findChild("build");
                if (buildClasses != null) {
                    VirtualFile classes = buildClasses.findChild("classes");
                    if (classes != null && classes.exists()) {
                        classPaths.add(classes.getPath());
                    }
                }
                
                VirtualFile targetClasses = outputRoot.findChild("target");
                if (targetClasses != null) {
                    VirtualFile classes = targetClasses.findChild("classes");
                    if (classes != null && classes.exists()) {
                        classPaths.add(classes.getPath());
                    }
                }
            }
        }
        
        return classPaths;
    }
}
