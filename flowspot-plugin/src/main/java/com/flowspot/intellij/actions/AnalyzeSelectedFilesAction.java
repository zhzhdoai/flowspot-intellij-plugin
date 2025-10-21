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
import java.util.Arrays;

/**
 * 分析选中文件操作
 * 对用户选中的文件执行 FlowSpot 安全分析
 */
public class AnalyzeSelectedFilesAction extends AnAction {
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        
        boolean enabled = project != null && !project.isDisposed() && 
                         selectedFiles != null && selectedFiles.length > 0;
        
        e.getPresentation().setEnabledAndVisible(enabled);
        
        if (enabled) {
            int fileCount = selectedFiles.length;
            String text = fileCount == 1 ? "Analyze Selected File" : "Analyze Selected Files (" + fileCount + ")";
            e.getPresentation().setText(text);
        }
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        
        if (project == null || selectedFiles == null || selectedFiles.length == 0) {
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
        logManager.logInfo("=== 选中文件分析操作中获取到的规则 ===");
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
                    indicator.setText("Preparing FlowSpot analysis for selected files...");
                    indicator.setIndeterminate(false);
                    indicator.setFraction(0.0);
                    
                    // 收集选中文件的路径
                    List<String> sourcePaths = collectSelectedFilePaths(selectedFiles);
                    List<String> classPaths = collectClassPaths(project, selectedFiles);
                    
                    if (sourcePaths.isEmpty()) {
                        indicator.setText("No valid source files selected");
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
                    
                    indicator.setText("Analysis completed for " + sourcePaths.size() + " files");
                    indicator.setFraction(1.0);
                    
                } catch (Exception ex) {
                    // 错误处理
                    indicator.setText("Analysis failed: " + ex.getMessage());
                }
            }
        });
    }
    
    /**
     * 收集选中文件的路径
     */
    @NotNull
    private List<String> collectSelectedFilePaths(@NotNull VirtualFile[] selectedFiles) {
        List<String> sourcePaths = new ArrayList<>();
        
        for (VirtualFile file : selectedFiles) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    // 如果是目录，添加目录路径
                    sourcePaths.add(file.getPath());
                } else {
                    // 如果是文件，添加文件所在目录
                    VirtualFile parent = file.getParent();
                    if (parent != null && !sourcePaths.contains(parent.getPath())) {
                        sourcePaths.add(parent.getPath());
                    }
                }
            }
        }
        
        return sourcePaths;
    }
    
    /**
     * 收集相关的类路径
     */
    @NotNull
    private List<String> collectClassPaths(@NotNull Project project, @NotNull VirtualFile[] selectedFiles) {
        List<String> classPaths = new ArrayList<>();
        
        // 对于选中的文件，尝试找到对应的输出目录
        for (VirtualFile file : selectedFiles) {
            VirtualFile root = findProjectRoot(file, project);
            if (root != null) {
                // 查找 build/classes 或 target/classes 目录
                VirtualFile buildClasses = root.findChild("build");
                if (buildClasses != null) {
                    VirtualFile classes = buildClasses.findChild("classes");
                    if (classes != null && classes.exists() && !classPaths.contains(classes.getPath())) {
                        classPaths.add(classes.getPath());
                    }
                }
                
                VirtualFile targetClasses = root.findChild("target");
                if (targetClasses != null) {
                    VirtualFile classes = targetClasses.findChild("classes");
                    if (classes != null && classes.exists() && !classPaths.contains(classes.getPath())) {
                        classPaths.add(classes.getPath());
                    }
                }
            }
        }
        
        return classPaths;
    }
    
    /**
     * 查找文件所属的项目根目录
     */
    private VirtualFile findProjectRoot(@NotNull VirtualFile file, @NotNull Project project) {
        VirtualFile current = file.isDirectory() ? file : file.getParent();
        
        while (current != null) {
            // 检查是否包含构建文件
            if (current.findChild("build.gradle") != null || 
                current.findChild("pom.xml") != null ||
                current.findChild("build.xml") != null) {
                return current;
            }
            current = current.getParent();
        }
        
        // 如果没找到，返回项目基础路径
        String basePath = project.getBasePath();
        if (basePath != null) {
            return file.getFileSystem().findFileByPath(basePath);
        }
        
        return null;
    }
}
