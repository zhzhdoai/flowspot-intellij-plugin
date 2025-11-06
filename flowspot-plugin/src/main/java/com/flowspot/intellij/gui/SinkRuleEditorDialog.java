/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.gui;

import com.flowspot.intellij.model.MethodPattern;
import com.flowspot.intellij.model.SinkPattern;
import com.flowspot.intellij.model.SinkRule;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Dialog for creating/editing a single sink rule.
 */
public class SinkRuleEditorDialog extends DialogWrapper {
    
    private final SinkRule originalRule;
    private JTextField nameField;
    private JComboBox<String> typeNameCombo;
    private JSpinner prioritySpinner;
    private JTextField categoryField;
    private JTextArea descriptionArea;
    private JTextArea patternsArea; // Simplified: JSON text area for patterns
    
    public SinkRuleEditorDialog(@Nullable Project project, @Nullable SinkRule rule) {
        super(project);
        this.originalRule = rule;
        
        setTitle(rule == null ? "Add New Sink Rule" : "Edit Sink Rule");
        init();
        
        if (rule != null) {
            loadRule(rule);
        }
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Name
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel namePanel = new JPanel(new BorderLayout());
        nameField = new JTextField(30);
        // 自动转换为大写
        nameField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String text = nameField.getText();
                String upperText = text.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
                if (!text.equals(upperText)) {
                    int pos = nameField.getCaretPosition();
                    nameField.setText(upperText);
                    nameField.setCaretPosition(Math.min(pos, upperText.length()));
                }
            }
        });
        namePanel.add(nameField, BorderLayout.CENTER);
        JLabel nameHint = new JLabel("(Auto-converts to uppercase, e.g., CUSTOM_SQL_INJECTION)");
        nameHint.setFont(nameHint.getFont().deriveFont(10f));
        namePanel.add(nameHint, BorderLayout.SOUTH);
        panel.add(namePanel, gbc);
        
        // Type Name
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        typeNameCombo = new JComboBox<>(new String[]{"CALL", "FIELD", "RETURN"});
        panel.add(typeNameCombo, gbc);
        
        // Priority
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(new JLabel("Priority (1-10):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        prioritySpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
        panel.add(prioritySpinner, gbc);
        
        // Category
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        categoryField = new JTextField(30);
        panel.add(categoryField, gbc);
        
        // Description
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 0.3;
        gbc.fill = GridBagConstraints.BOTH;
        descriptionArea = new JTextArea(3, 30);
        panel.add(new JScrollPane(descriptionArea), gbc);
        
        // Patterns (simplified as text area)
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Method Patterns:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 0.7;
        gbc.fill = GridBagConstraints.BOTH;
        patternsArea = new JTextArea(10, 30);
        patternsArea.setToolTipText("Format: method_regex|param1,param2 (one per line)");
        panel.add(new JScrollPane(patternsArea), gbc);
        
        panel.setPreferredSize(new Dimension(600, 500));
        return panel;
    }
    
    private void loadRule(SinkRule rule) {
        nameField.setText(rule.getName());
        typeNameCombo.setSelectedItem(rule.getTypeName());
        prioritySpinner.setValue(rule.getPriority());
        categoryField.setText(rule.getCategory());
        descriptionArea.setText(rule.getDescription());
        
        // Load patterns (simplified)
        StringBuilder sb = new StringBuilder();
        for (SinkPattern sink : rule.getSinks()) {
            for (MethodPattern pattern : sink.getPatterns()) {
                sb.append(pattern.getMethod()).append("|");
                sb.append(String.join(",", pattern.getTaintedParams()));
                sb.append("\n");
            }
        }
        patternsArea.setText(sb.toString());
    }
    
    public SinkRule getRule() {
        SinkRule rule = new SinkRule();
        rule.setName(nameField.getText().trim());
        rule.setTypeName((String) typeNameCombo.getSelectedItem());
        rule.setPriority((Integer) prioritySpinner.getValue());
        rule.setCategory(categoryField.getText().trim());
        rule.setDescription(descriptionArea.getText().trim());
        
        // Parse patterns (simplified)
        List<MethodPattern> methodPatterns = new ArrayList<>();
        String[] lines = patternsArea.getText().split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\\|");
            if (parts.length >= 1) {
                String method = parts[0].trim();
                List<String> params = parts.length > 1 
                    ? Arrays.asList(parts[1].split(","))
                    : new ArrayList<>();
                methodPatterns.add(new MethodPattern(method, params));
            }
        }
        
        SinkPattern sinkPattern = new SinkPattern(rule.getTypeName(), methodPatterns);
        rule.setSinks(Arrays.asList(sinkPattern));
        
        return rule;
    }
    
    @Override
    protected ValidationInfo doValidate() {
        if (nameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Name cannot be empty", nameField);
        }
        if (categoryField.getText().trim().isEmpty()) {
            return new ValidationInfo("Category cannot be empty", categoryField);
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            return new ValidationInfo("Description cannot be empty", descriptionArea);
        }
        return null;
    }
}
