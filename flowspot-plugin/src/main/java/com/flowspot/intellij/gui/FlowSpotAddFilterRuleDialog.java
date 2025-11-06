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

import com.flowspot.intellij.model.FlowSpotFilterRule;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 添加 FlowSpot 过滤规则对话框
 */
public class FlowSpotAddFilterRuleDialog extends JDialog {
    
    private final JTextField classNameField;
    private final JTextField methodNameField;
    private final JTextField descriptionField;
    private final JComboBox<FlowSpotFilterRule.FilterType> filterTypeCombo;
    private FlowSpotFilterRule createdRule = null;
    
    public FlowSpotAddFilterRuleDialog(Dialog parent) {
        super(parent, "Add Filter Rule", true);
        
        // 初始化组件
        classNameField = new JTextField(30);
        methodNameField = new JTextField(30);
        descriptionField = new JTextField(30);
        filterTypeCombo = new JComboBox<>(FlowSpotFilterRule.FilterType.values());
        filterTypeCombo.setSelectedItem(FlowSpotFilterRule.FilterType.EXACT_MATCH);
        
        initializeUI();
        setupEventHandlers();
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 主面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // 过滤类型
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Filter Type:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(filterTypeCombo, gbc);
        
        // 类名
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Class Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(classNameField, gbc);
        
        // 方法名
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Method Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(methodNameField, gbc);
        
        // 描述
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(descriptionField, gbc);
        
        // 帮助文本
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextArea helpText = new JTextArea(
            "Filter Type Descriptions:\n" +
            "• Exact Match: Matches both class name and method name exactly\n" +
            "• Class Match: Matches class name only (method name ignored)\n" +
            "• Method Match: Matches method name only (class name ignored)\n" +
            "• Contains Match: Matches if class or method name contains the specified text"
        );
        helpText.setEditable(false);
        helpText.setOpaque(false);
        helpText.setFont(helpText.getFont().deriveFont(Font.PLAIN, 11f));
        helpText.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        mainPanel.add(helpText, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        okButton.addActionListener(e -> handleOK());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置默认按钮
        getRootPane().setDefaultButton(okButton);
    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 过滤类型变化时更新字段状态
        filterTypeCombo.addActionListener(e -> updateFieldStates());
        
        // 初始化字段状态
        updateFieldStates();
    }
    
    /**
     * 根据过滤类型更新字段状态
     */
    private void updateFieldStates() {
        FlowSpotFilterRule.FilterType selectedType = 
            (FlowSpotFilterRule.FilterType) filterTypeCombo.getSelectedItem();
        
        if (selectedType != null) {
            switch (selectedType) {
                case EXACT_MATCH:
                    classNameField.setEnabled(true);
                    methodNameField.setEnabled(true);
                    setFieldPlaceholder(classNameField, "com.example.MyClass");
                    setFieldPlaceholder(methodNameField, "myMethod");
                    break;
                    
                case CLASS_MATCH:
                    classNameField.setEnabled(true);
                    methodNameField.setEnabled(false);
                    methodNameField.setText("*");
                    setFieldPlaceholder(classNameField, "com.example.MyClass");
                    break;
                    
                case METHOD_MATCH:
                    classNameField.setEnabled(false);
                    classNameField.setText("*");
                    methodNameField.setEnabled(true);
                    setFieldPlaceholder(methodNameField, "myMethod");
                    break;
                    
                case CONTAINS_MATCH:
                    classNameField.setEnabled(true);
                    methodNameField.setEnabled(true);
                    setFieldPlaceholder(classNameField, "MyClass (partial match)");
                    setFieldPlaceholder(methodNameField, "method (partial match)");
                    break;
            }
        }
    }
    
    /**
     * 设置字段占位符文本
     */
    private void setFieldPlaceholder(JTextField field, String placeholder) {
        if (field.getText().isEmpty()) {
            field.setToolTipText("Example: " + placeholder);
        }
    }
    
    /**
     * 处理OK按钮点击
     */
    private void handleOK() {
        // 验证输入
        String className = classNameField.getText().trim();
        String methodName = methodNameField.getText().trim();
        String description = descriptionField.getText().trim();
        FlowSpotFilterRule.FilterType filterType = 
            (FlowSpotFilterRule.FilterType) filterTypeCombo.getSelectedItem();
        
        // 基本验证
        if (filterType == null) {
            showError("Please select a filter type.");
            return;
        }
        
        // 根据过滤类型验证必需字段
        switch (filterType) {
            case EXACT_MATCH:
                if (className.isEmpty() || methodName.isEmpty()) {
                    showError("Both class name and method name are required for exact match.");
                    return;
                }
                break;
                
            case CLASS_MATCH:
                if (className.isEmpty()) {
                    showError("Class name is required for class match.");
                    return;
                }
                methodName = "*"; // 确保方法名为通配符
                break;
                
            case METHOD_MATCH:
                if (methodName.isEmpty()) {
                    showError("Method name is required for method match.");
                    return;
                }
                className = "*"; // 确保类名为通配符
                break;
                
            case CONTAINS_MATCH:
                if (className.isEmpty() && methodName.isEmpty()) {
                    showError("At least one of class name or method name is required for contains match.");
                    return;
                }
                // 对于包含匹配，空字段用通配符替代
                if (className.isEmpty()) className = "*";
                if (methodName.isEmpty()) methodName = "*";
                break;
        }
        
        // 创建过滤规则
        try {
            createdRule = new FlowSpotFilterRule(className, methodName, description, filterType);
            dispose();
        } catch (Exception e) {
            showError("Failed to create filter rule: " + e.getMessage());
        }
    }
    
    /**
     * 显示错误消息
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Invalid Input",
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    /**
     * 获取创建的规则
     */
    @Nullable
    public FlowSpotFilterRule getCreatedRule() {
        return createdRule;
    }
}
