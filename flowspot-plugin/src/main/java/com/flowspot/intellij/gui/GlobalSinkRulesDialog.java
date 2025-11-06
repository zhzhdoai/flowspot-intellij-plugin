/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.gui;

import com.flowspot.intellij.exception.ValidationException;
import com.flowspot.intellij.model.SinkRule;
import com.flowspot.intellij.service.GlobalSinkRulesService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main dialog for managing global sink rules.
 * Provides table view with Add/Edit/Delete/Import/Export actions.
 */
public class GlobalSinkRulesDialog extends DialogWrapper {
    
    private final Project project;
    private final GlobalSinkRulesService service;
    
    public GlobalSinkRulesDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        this.service = GlobalSinkRulesService.getInstance();
        
        setTitle("Global Sink Rules Configuration");
        setModal(true);
        init();
    }
    
    private JTextArea jsonEditor;
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(900, 600));
        
        // 标题和说明
        JLabel titleLabel = new JLabel("Edit Global Sink Rules (JSON Format)");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // JSON 编辑器
        jsonEditor = new JTextArea();
        jsonEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        jsonEditor.setTabSize(2);
        
        // 加载当前规则
        String currentJson = service.getRulesAsJson();
        
        // 格式化 JSON
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Object jsonObject = gson.fromJson(currentJson, Object.class);
            String formattedJson = gson.toJson(jsonObject);
            jsonEditor.setText(formattedJson);
        } catch (Exception e) {
            jsonEditor.setText(currentJson);
        }
        
        JScrollPane scrollPane = new JScrollPane(jsonEditor);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 底部按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton validateButton = new JButton("Validate JSON");
        validateButton.addActionListener(e -> validateJson());
        JButton formatButton = new JButton("Format");
        formatButton.addActionListener(e -> formatJson());
        buttonPanel.add(validateButton);
        buttonPanel.add(formatButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void validateJson() {
        String json = jsonEditor.getText();
        try {
            // 只验证 JSON 语法
            com.google.gson.Gson gson = new com.google.gson.Gson();
            gson.fromJson(json, Object.class);
            Messages.showInfoMessage("JSON syntax is valid!", "Validation Success");
        } catch (Exception e) {
            Messages.showErrorDialog(
                "Invalid JSON syntax:\n" + e.getMessage(),
                "JSON Syntax Error"
            );
        }
    }
    
    private void formatJson() {
        String json = jsonEditor.getText();
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Object jsonObject = gson.fromJson(json, Object.class);
            jsonEditor.setText(gson.toJson(jsonObject));
        } catch (Exception e) {
            Messages.showErrorDialog("Invalid JSON syntax: " + e.getMessage(), "Format Error");
        }
    }
    
    @Override
    protected void doOKAction() {
        // 保存 JSON（只验证语法）
        String json = jsonEditor.getText();
        try {
            // 验证 JSON 语法
            com.google.gson.Gson gson = new com.google.gson.Gson();
            gson.fromJson(json, Object.class);
            
            // 保存到全局 state
            service.importRulesFromJson(json, true);
            
            // 复制到所有打开项目的 .flowspot/config/sinks.json
            copyGlobalRulesToAllProjects();
            
            super.doOKAction();
        } catch (com.google.gson.JsonSyntaxException e) {
            Messages.showErrorDialog(
                "Invalid JSON syntax:\n" + e.getMessage(),
                "JSON Syntax Error"
            );
        } catch (Exception e) {
            Messages.showErrorDialog(
                "Failed to save:\n" + e.getMessage(),
                "Save Error"
            );
        }
    }
    
    /**
     * 将全局规则复制到所有打开项目的 .flowspot/config/sinks.json
     */
    private void copyGlobalRulesToAllProjects() {
        try {
            com.intellij.openapi.project.Project[] openProjects = 
                com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects();

            
            int successCount = 0;
            for (com.intellij.openapi.project.Project project : openProjects) {
                try {
                    String basePath = project.getBasePath();
                    
                    if (basePath != null) {
                        // 创建配置目录
                        java.nio.file.Path configDir = java.nio.file.Paths.get(basePath, ".flowspot", "config");
                        
                        if (!java.nio.file.Files.exists(configDir)) {
                            java.nio.file.Files.createDirectories(configDir);
                        }
                        
                        // 写入 sinks.json
                        java.nio.file.Path sinksPath = configDir.resolve("sinks.json");
                        String globalJson = service.getRulesAsJson();

                        
                        java.nio.file.Files.writeString(sinksPath, globalJson, 
                            java.nio.charset.StandardCharsets.UTF_8);

                        successCount++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
