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
import com.flowspot.intellij.service.FlowSpotFilterManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FlowSpot 过滤规则管理对话框
 */
public class FlowSpotFilterManagementDialog extends JDialog {
    
    private final Project project;
    private final FlowSpotFilterManager filterManager;
    private final FilterRuleTableModel tableModel;
    private final JTable filterTable;
    private boolean rulesChanged = false;
    
    public FlowSpotFilterManagementDialog(@NotNull Project project, @NotNull FlowSpotFilterManager filterManager) {
        super((Frame) null, "Manage FlowSpot Filter Rules", true);
        this.project = project;
        this.filterManager = filterManager;
        
        // 创建表格模型和表格
        this.tableModel = new FilterRuleTableModel();
        this.filterTable = new JTable(tableModel);
        
        initializeUI();
        loadFilterRules();
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 顶部信息面板
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.NORTH);
        
        // 中央表格面板
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 创建信息面板
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("FlowSpot Data Flow Filter Rules");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(titleLabel, BorderLayout.WEST);
        
        JLabel countLabel = new JLabel();
        updateCountLabel(countLabel);
        panel.add(countLabel, BorderLayout.EAST);
        
        JTextArea descriptionArea = new JTextArea(
            "Filter rules hide vulnerabilities that contain matching data flow nodes.\n" +
            "Rules are automatically applied to all future analysis results."
        );
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setFont(descriptionArea.getFont().deriveFont(Font.PLAIN, 12f));
        panel.add(descriptionArea, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建表格面板
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // 配置表格
        filterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        filterTable.setRowHeight(25);
        filterTable.getTableHeader().setReorderingAllowed(false);
        
        // 设置列宽
        filterTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Type
        filterTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Rule
        filterTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Description
        filterTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Created
        
        // 设置类型列的渲染器
        filterTable.getColumnModel().getColumn(0).setCellRenderer(new FilterTypeRenderer());
        
        JScrollPane scrollPane = new JScrollPane(filterTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        JButton addButton = new JButton("Add Rule...");
        addButton.addActionListener(e -> showAddRuleDialog());
        panel.add(addButton);
        
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSelectedRule());
        panel.add(removeButton);
        
        JButton clearAllButton = new JButton("Clear All");
        clearAllButton.addActionListener(e -> clearAllRules());
        panel.add(clearAllButton);
        
        panel.add(Box.createHorizontalStrut(20));
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        
        return panel;
    }
    
    /**
     * 加载过滤规则
     */
    private void loadFilterRules() {
        Set<FlowSpotFilterRule> rules = filterManager.getAllFilterRules();
        tableModel.setRules(new ArrayList<>(rules));
    }
    
    /**
     * 更新计数标签
     */
    private void updateCountLabel(JLabel label) {
        int count = filterManager.getFilterRuleCount();
        label.setText(count + " rule" + (count != 1 ? "s" : ""));
    }
    
    /**
     * 显示添加规则对话框
     */
    private void showAddRuleDialog() {
        FlowSpotAddFilterRuleDialog dialog = new FlowSpotAddFilterRuleDialog(this);
        dialog.setVisible(true);
        
        FlowSpotFilterRule newRule = dialog.getCreatedRule();
        if (newRule != null) {
            filterManager.addFilterRule(newRule);
            loadFilterRules();
            rulesChanged = true;
        }
    }
    
    /**
     * 移除选中的规则
     */
    private void removeSelectedRule() {
        int selectedRow = filterTable.getSelectedRow();
        if (selectedRow >= 0) {
            FlowSpotFilterRule rule = tableModel.getRuleAt(selectedRow);
            
            int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove this filter rule?\n\n" +
                "Rule: " + rule.getDisplayName() + "\n" +
                "Description: " + rule.getDescription(),
                "Remove Filter Rule",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == JOptionPane.YES_OPTION) {
                filterManager.removeFilterRule(rule);
                loadFilterRules();
                rulesChanged = true;
            }
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Please select a filter rule to remove.",
                "No Selection",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
    
    /**
     * 清空所有规则
     */
    private void clearAllRules() {
        int count = filterManager.getFilterRuleCount();
        if (count == 0) {
            JOptionPane.showMessageDialog(
                this,
                "No filter rules to clear.",
                "No Rules",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to remove all " + count + " filter rules?\n\n" +
            "This action cannot be undone.",
            "Clear All Filter Rules",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            filterManager.clearAllFilterRules();
            loadFilterRules();
            rulesChanged = true;
        }
    }
    
    /**
     * 检查规则是否有变化
     */
    public boolean isRulesChanged() {
        return rulesChanged;
    }
    
    /**
     * 过滤规则表格模型
     */
    private static class FilterRuleTableModel extends AbstractTableModel {
        
        private final String[] columnNames = {"Type", "Rule", "Description", "Created"};
        private List<FlowSpotFilterRule> rules = new ArrayList<>();
        
        public void setRules(List<FlowSpotFilterRule> rules) {
            this.rules = rules;
            fireTableDataChanged();
        }
        
        public FlowSpotFilterRule getRuleAt(int row) {
            return rules.get(row);
        }
        
        @Override
        public int getRowCount() {
            return rules.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            FlowSpotFilterRule rule = rules.get(rowIndex);
            switch (columnIndex) {
                case 0: return rule.getFilterType();
                case 1: return rule.getDisplayName();
                case 2: return rule.getDescription();
                case 3: return rule.getCreatedTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                default: return null;
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
    
    /**
     * 过滤类型渲染器
     */
    private static class FilterTypeRenderer extends DefaultTableCellRenderer {
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof FlowSpotFilterRule.FilterType) {
                FlowSpotFilterRule.FilterType type = (FlowSpotFilterRule.FilterType) value;
                switch (type) {
                    case EXACT_MATCH:
                        setText("Exact");
                        setToolTipText("Exact match of class and method");
                        break;
                    case CLASS_MATCH:
                        setText("Class");
                        setToolTipText("Match by class name only");
                        break;
                    case METHOD_MATCH:
                        setText("Method");
                        setToolTipText("Match by method name only");
                        break;
                    case CONTAINS_MATCH:
                        setText("Contains");
                        setToolTipText("Contains match in class or method name");
                        break;
                    default:
                        setText(type.name());
                        break;
                }
            }
            
            return this;
        }
    }
}
