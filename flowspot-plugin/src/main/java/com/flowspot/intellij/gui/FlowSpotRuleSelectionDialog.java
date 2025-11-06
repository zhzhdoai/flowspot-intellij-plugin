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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.flowspot.intellij.core.FlowSpotLogManager;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.flowspot.intellij.core.FlowSpotRuleLoader;
import com.flowspot.intellij.core.FlowSpotConfigManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * FlowSpot 规则选择对话框
 * 允许用户选择要扫描的 sources 和 sinks 规则
 */
public class FlowSpotRuleSelectionDialog extends DialogWrapper {
    
    private final Project project;
    private final FlowSpotLogManager logManager;
    private CheckboxTree sourceRulesTree;
    private CheckboxTree sinkRulesTree;
    private final Set<String> selectedSourceRules;
    private final Set<String> selectedSinkRules;
    
    // 优化配置UI组件
    private JBCheckBox enableSubPathDeduplicationCheckBox;
    private JBCheckBox enableSinkLocationDeduplicationCheckBox;
    private JBCheckBox enableContextFilteringCheckBox;
    private JPanel optimizationPanel;
    private boolean optimizationPanelExpanded = false;
    
    // 动态加载的规则数据
    private Map<String, FlowSpotRuleLoader.SourceCategory> sourceCategories;
    private Map<String, FlowSpotRuleLoader.SinkCategory> sinkCategories;
    
    // 配置管理器
    private FlowSpotConfigManager configManager;
    
    public FlowSpotRuleSelectionDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        this.logManager = FlowSpotLogManager.getInstance(project);
        this.selectedSourceRules = new HashSet<>();
        this.selectedSinkRules = new HashSet<>();
        
        setTitle("FlowSpot Rule Selection");
        setModal(true);
        setResizable(true);
        
        init();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.setPreferredSize(JBUI.size(1000, 600));
        
        // 动态加载规则数据
        loadRulesData();
        
        // 创建左右分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5); // 左右各占50%
        splitPane.setDividerLocation(0.5);
        
        // Sources 面板（左侧）
        JBPanel<?> sourcesPanel = createSourcesPanel();
        sourcesPanel.setBorder(BorderFactory.createTitledBorder("Sources"));
        splitPane.setLeftComponent(sourcesPanel);
        
        // Sinks 面板（右侧）
        JBPanel<?> sinksPanel = createSinksPanel();
        sinksPanel.setBorder(BorderFactory.createTitledBorder("Sinks"));
        splitPane.setRightComponent(sinksPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // 创建底部面板，包含优化配置和按钮
        JBPanel<?> bottomPanel = new JBPanel<>(new BorderLayout());
        
        // 优化配置面板
        JBPanel<?> optimizationConfigPanel = createOptimizationConfigPanel();
        bottomPanel.add(optimizationConfigPanel, BorderLayout.NORTH);
        
        // 底部按钮面板
        JBPanel<?> buttonPanel = createButtonPanel();
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // 默认折叠所有节点
        collapseAllTrees();
        
        return mainPanel;
    }
    
    /**
     * 动态加载规则数据
     */
    private void loadRulesData() {
        try {
            // 初始化配置管理器
            configManager = new FlowSpotConfigManager(project);
            
            // 获取有效的sinks.json配置路径
            String effectiveSinksJsonPath = configManager.getEffectiveSinksJsonPath();
            
            logManager.logInfo("Loading rules from: " + effectiveSinksJsonPath);
            
            // 动态加载 Source 规则
            sourceCategories = FlowSpotRuleLoader.loadSourceCategories();
            logManager.logInfo("Loaded " + sourceCategories.size() + " source categories");
            
            // 动态加载 Sink 规则（使用全局配置）
            sinkCategories = FlowSpotRuleLoader.loadSinkCategories(effectiveSinksJsonPath);
            logManager.logInfo("Loaded " + sinkCategories.size() + " sink categories");
            
            // 如果全局规则为空，显示提示
            if (sinkCategories.isEmpty()) {
                logManager.logWarning("No global sink rules configured. Please configure global rules first.");
            }
            
        } catch (Exception e) {
            logManager.logError("Failed to load rules data", e);
            // 如果加载失败，使用空的分类
            sourceCategories = new HashMap<>();
            sinkCategories = new HashMap<>();
        }
    }
    
    /**
     * 创建 Sources 面板
     */
    private JBPanel<?> createSourcesPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // 描述标签
        JBLabel descLabel = new JBLabel("Select source rules to identify data entry points:");
        descLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(descLabel, BorderLayout.NORTH);
        
        // 创建 Sources 树
        sourceRulesTree = createSourceTree();
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(sourceRulesTree);
        scrollPane.setPreferredSize(JBUI.size(750, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 统计信息
        int totalRules = sourceCategories.values().stream().mapToInt(FlowSpotRuleLoader.SourceCategory::getCount).sum();
        JBLabel statsLabel = new JBLabel(String.format("Total: %d categories, %d rules", 
                                                      sourceCategories.size(), totalRules));
        statsLabel.setBorder(JBUI.Borders.emptyTop(5));
        panel.add(statsLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建 Sinks 面板
     */
    private JBPanel<?> createSinksPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));
        
        // 描述标签
        JBLabel descLabel = new JBLabel("Select sink rules to identify potential vulnerability points (默认已选择高分规则 score>6):");
        descLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(descLabel, BorderLayout.NORTH);
        
        // 创建 Sinks 树
        sinkRulesTree = createSinkTree();
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(sinkRulesTree);
        scrollPane.setPreferredSize(JBUI.size(750, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 统计信息
        int totalRules = sinkCategories.values().stream().mapToInt(FlowSpotRuleLoader.SinkCategory::getTotalCount).sum();
        int highScoreRules = countHighScoreRules();
        JBLabel statsLabel = new JBLabel(String.format("Total: %d categories, %d rules (默认选择 %d 个高分规则 score>6)", 
                                                      sinkCategories.size(), totalRules, highScoreRules));
        statsLabel.setBorder(JBUI.Borders.emptyTop(5));
        panel.add(statsLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 创建 Source 规则树
     */
    private CheckboxTree createSourceTree() {
        CheckedTreeNode root = new CheckedTreeNode("Source Rules");
        
        for (FlowSpotRuleLoader.SourceCategory category : sourceCategories.values()) {
            CheckedTreeNode categoryNode = new CheckedTreeNode(
                String.format("%s (%d)", category.getName(), category.getRules().size()));
            categoryNode.setChecked(false); // 修改：默认不选中，让用户主动选择
            
            for (omni.scan.Query rule : category.getRules()) {
                CheckedTreeNode ruleNode = new CheckedTreeNode(rule.name());
                ruleNode.setChecked(false); // 修改：默认不选中，让用户主动选择
                categoryNode.add(ruleNode);
            }
            
            root.add(categoryNode);
        }
        
        CheckboxTree tree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof CheckedTreeNode) {
                    CheckedTreeNode node = (CheckedTreeNode) value;
                    getTextRenderer().append(node.getUserObject().toString());
                }
            }
        }, root);
        
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        
        // 展开所有节点
        expandAllNodes(tree, root);
        
        return tree;
    }
    
    /**
     * 创建 Sink 规则树
     */
    private CheckboxTree createSinkTree() {
        CheckedTreeNode root = new CheckedTreeNode("Sink Rules");
        
        for (FlowSpotRuleLoader.SinkCategory category : sinkCategories.values()) {
            CheckedTreeNode categoryNode = new CheckedTreeNode(
                String.format("%s (%d)", category.getCategory(), category.getTotalCount()));
            
            boolean hasHighScoreRules = false;
            
            for (Map.Entry<String, List<omni.scan.Query>> subEntry : category.getSubCategories().entrySet()) {
                // 每个name只显示一次，不显示具体的Query
                CheckedTreeNode nameNode = new CheckedTreeNode(subEntry.getKey());
                
                // 检查该规则组中是否有score > 6的规则
                boolean shouldSelectByDefault = subEntry.getValue().stream()
                    .anyMatch(query -> query.score() > 6.0);
                
                nameNode.setChecked(shouldSelectByDefault);
                if (shouldSelectByDefault) {
                    hasHighScoreRules = true;
                }
                
                categoryNode.add(nameNode);
            }
            
            // 如果分类下有高分规则被选中，则分类节点也选中
            categoryNode.setChecked(hasHighScoreRules);
            root.add(categoryNode);
        }
        
        CheckboxTree tree = new CheckboxTree(new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (value instanceof CheckedTreeNode) {
                    CheckedTreeNode node = (CheckedTreeNode) value;
                    getTextRenderer().append(node.getUserObject().toString());
                }
            }
        }, root);
        
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        
        // 展开所有节点
        expandAllNodes(tree, root);
        
        return tree;
    }
    
    /**
     * 展开所有节点
     */
    private void expandAllNodes(CheckboxTree tree, CheckedTreeNode node) {
        tree.expandPath(new TreePath(node.getPath()));
        for (int i = 0; i < node.getChildCount(); i++) {
            CheckedTreeNode child = (CheckedTreeNode) node.getChildAt(i);
            expandAllNodes(tree, child);
        }
    }
    
    /**
     * 统计高分规则数量（score > 6）
     */
    private int countHighScoreRules() {
        int count = 0;
        for (FlowSpotRuleLoader.SinkCategory category : sinkCategories.values()) {
            for (Map.Entry<String, List<omni.scan.Query>> subEntry : category.getSubCategories().entrySet()) {
                boolean hasHighScore = subEntry.getValue().stream()
                    .anyMatch(query -> query.score() > 6.0);
                if (hasHighScore) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * 创建按钮面板
     */
    private JBPanel<?> createButtonPanel() {
        JBPanel<?> buttonPanel = new JBPanel<>(new FlowLayout());
        
        JButton selectAllSourcesBtn = new JButton("Select All Sources");
        selectAllSourcesBtn.addActionListener(e -> selectAllTreeNodes(sourceRulesTree));
        
        JButton deselectAllSourcesBtn = new JButton("Deselect All Sources");
        deselectAllSourcesBtn.addActionListener(e -> deselectAllTreeNodes(sourceRulesTree));
        
        JButton selectAllSinksBtn = new JButton("Select All Sinks");
        selectAllSinksBtn.addActionListener(e -> selectAllTreeNodes(sinkRulesTree));
        
        JButton deselectAllSinksBtn = new JButton("Deselect All Sinks");
        deselectAllSinksBtn.addActionListener(e -> deselectAllTreeNodes(sinkRulesTree));
        
        JButton selectHighScoreSinksBtn = new JButton("Select High Score Sinks (>6)");
        selectHighScoreSinksBtn.addActionListener(e -> selectHighScoreSinks());
        
        buttonPanel.add(selectAllSourcesBtn);
        buttonPanel.add(deselectAllSourcesBtn);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(selectAllSinksBtn);
        buttonPanel.add(deselectAllSinksBtn);
        buttonPanel.add(selectHighScoreSinksBtn);
        
        return buttonPanel;
    }
    
    
    /**
     * 选择树中的所有节点
     */
    private void selectAllTreeNodes(CheckboxTree tree) {
        CheckedTreeNode root = (CheckedTreeNode) tree.getModel().getRoot();
        setAllNodesChecked(root, true);
        tree.repaint();
    }
    
    /**
     * 取消选择树中的所有节点
     */
    private void deselectAllTreeNodes(CheckboxTree tree) {
        CheckedTreeNode root = (CheckedTreeNode) tree.getModel().getRoot();
        setAllNodesChecked(root, false);
        tree.repaint();
    }
    
    /**
     * 递归设置所有节点的选中状态
     */
    private void setAllNodesChecked(CheckedTreeNode node, boolean checked) {
        node.setChecked(checked);
        for (int i = 0; i < node.getChildCount(); i++) {
            CheckedTreeNode child = (CheckedTreeNode) node.getChildAt(i);
            setAllNodesChecked(child, checked);
        }
    }
    
    /**
     * 选择高分Sink规则（score > 6）
     */
    private void selectHighScoreSinks() {
        CheckedTreeNode root = (CheckedTreeNode) sinkRulesTree.getModel().getRoot();
        
        // 首先取消所有选择
        setAllNodesChecked(root, false);
        
        // 然后选择高分规则
        for (int i = 0; i < root.getChildCount(); i++) {
            CheckedTreeNode categoryNode = (CheckedTreeNode) root.getChildAt(i);
            boolean hasHighScoreRules = false;
            
            for (int j = 0; j < categoryNode.getChildCount(); j++) {
                CheckedTreeNode nameNode = (CheckedTreeNode) categoryNode.getChildAt(j);
                String ruleName = nameNode.getUserObject().toString();
                
                // 查找对应的规则并检查score
                boolean shouldSelect = false;
                for (FlowSpotRuleLoader.SinkCategory category : sinkCategories.values()) {
                    List<omni.scan.Query> queries = category.getSubCategories().get(ruleName);
                    if (queries != null && queries.stream().anyMatch(query -> query.score() > 6.0)) {
                        shouldSelect = true;
                        break;
                    }
                }
                
                nameNode.setChecked(shouldSelect);
                if (shouldSelect) {
                    hasHighScoreRules = true;
                }
            }
            
            categoryNode.setChecked(hasHighScoreRules);
        }
        
        sinkRulesTree.repaint();
    }
    
    @Override
    protected void doOKAction() {
        // 收集选中的规则
        selectedSourceRules.clear();
        selectedSinkRules.clear();
        
        logManager.logDebug("=== 开始收集选中的规则 ===");
        logManager.logDebug("使用的日志目录: " + project.getBasePath() + "/.flowspot");
        
        // 收集选中的 source 规则
        collectSelectedSourceRules();
        logManager.logDebug("收集到的Source规则数量: " + selectedSourceRules.size());
        logManager.logDebug("收集到的Source规则: " + selectedSourceRules);
        
        // 收集选中的 sink 规则
        collectSelectedSinkRules();
        logManager.logDebug("收集到的Sink规则数量: " + selectedSinkRules.size());
        logManager.logDebug("收集到的Sink规则: " + selectedSinkRules);
        
        logManager.logDebug("是否有选中的规则: " + hasSelectedRules());
        
        super.doOKAction();
    }
    
    /**
     * 收集选中的 Source 规则
     */
    private void collectSelectedSourceRules() {
        CheckedTreeNode root = (CheckedTreeNode) sourceRulesTree.getModel().getRoot();
        Set<String> selectedItems = new HashSet<>();
        
        collectCheckedNodes(root, selectedItems);
        
        // 使用 FlowSpotRuleLoader 转换为实际的规则名称
        selectedSourceRules.addAll(FlowSpotRuleLoader.getSelectedSourceRules(selectedItems, sourceCategories));
    }
    
    /**
     * 收集选中的 Sink 规则
     */
    private void collectSelectedSinkRules() {
        CheckedTreeNode root = (CheckedTreeNode) sinkRulesTree.getModel().getRoot();
        Set<String> selectedItems = new HashSet<>();
        
        collectCheckedNodes(root, selectedItems);
        
        // 使用 FlowSpotRuleLoader 转换为实际的规则名称
        selectedSinkRules.addAll(FlowSpotRuleLoader.getSelectedSinkRules(selectedItems, sinkCategories));
    }
    
    /**
     * 递归收集选中的节点
     * 修复逻辑：收集所有选中的节点，包括分类节点和具体规则节点
     */
    private void collectCheckedNodes(CheckedTreeNode node, Set<String> selectedItems) {
        if (node.isChecked()) {
            String nodeText = node.getUserObject().toString();
            String itemName = extractCategoryName(nodeText);
            
            // 收集所有选中的节点，不管是分类还是具体规则
            selectedItems.add(itemName);
            logManager.logDebug("收集到选中的节点: " + itemName + " (是否叶子节点: " + node.isLeaf() + ")");
        }
        
        // 无论节点是否选中，都递归检查子节点（因为子节点可能单独选中）
        for (int i = 0; i < node.getChildCount(); i++) {
            CheckedTreeNode child = (CheckedTreeNode) node.getChildAt(i);
            collectCheckedNodes(child, selectedItems);
        }
    }
    
    /**
     * 从节点文本中提取分类名称（去掉统计信息）
     */
    private String extractCategoryName(String nodeText) {
        int parenIndex = nodeText.indexOf(" (");
        return parenIndex > 0 ? nodeText.substring(0, parenIndex) : nodeText;
    }
    
    /**
     * 获取选中的 source 规则
     */
    public Set<String> getSelectedSourceRules() {
        return new HashSet<>(selectedSourceRules);
    }
    
    /**
     * 获取选中的 sink 规则
     */
    public Set<String> getSelectedSinkRules() {
        return new HashSet<>(selectedSinkRules);
    }
    
    /**
     * 检查是否有选中的规则
     */
    public boolean hasSelectedRules() {
        return !selectedSourceRules.isEmpty() || !selectedSinkRules.isEmpty();
    }
    
    /**
     * 获取选择摘要信息
     */
    public String getSelectionSummary() {
        return String.format("Sources: %d, Sinks: %d", 
                           selectedSourceRules.size(), selectedSinkRules.size());
    }
    
    /**
     * 折叠所有树节点
     */
    private void collapseAllTrees() {
        if (sourceRulesTree != null) {
            collapseTree(sourceRulesTree);
        }
        if (sinkRulesTree != null) {
            collapseTree(sinkRulesTree);
        }
    }
    
    /**
     * 折叠指定树的所有节点
     */
    private void collapseTree(CheckboxTree tree) {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }
    
    /**
     * 创建优化配置面板
     */
    private JBPanel<?> createOptimizationConfigPanel() {
        JBPanel<?> panel = new JBPanel<>(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(5, 10));
        
        // 创建折叠/展开按钮
        JButton toggleButton = new JButton("▶ 高级优化选项");
        toggleButton.setFont(toggleButton.getFont().deriveFont(Font.PLAIN, 12f));
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
        
        // 创建优化选项内容面板
        optimizationPanel = createOptimizationOptionsPanel();
        optimizationPanel.setVisible(false); // 默认隐藏
        
        // 添加点击事件
        toggleButton.addActionListener(e -> {
            optimizationPanelExpanded = !optimizationPanelExpanded;
            optimizationPanel.setVisible(optimizationPanelExpanded);
            toggleButton.setText(optimizationPanelExpanded ? "▼ 高级优化选项" : "▶ 高级优化选项");
            
            // 重新调整对话框大小
            SwingUtilities.invokeLater(() -> {
                Window window = SwingUtilities.getWindowAncestor(panel);
                if (window != null) {
                    window.pack();
                }
            });
        });
        
        panel.add(toggleButton, BorderLayout.NORTH);
        panel.add(optimizationPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建优化选项内容面板
     */
    private JPanel createOptimizationOptionsPanel() {
        JBPanel<?> panel = new JBPanel<>(new GridBagLayout());
        panel.setBorder(JBUI.Borders.empty(10, 20, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(2, 0);
        
        // 说明标签
        JBLabel descLabel = new JBLabel("配置数据流分析的优化选项（默认全部启用，适合大多数场景）:");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC, 11f));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(descLabel, gbc);
        
        // 子路径去重选项
        enableSubPathDeduplicationCheckBox = new JBCheckBox("子路径去重", true);
        enableSubPathDeduplicationCheckBox.setToolTipText("移除被其他路径包含的子路径，保留更完整的数据流路径");
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(enableSubPathDeduplicationCheckBox, gbc);
        
        JBLabel subPathLabel = new JBLabel("移除被包含的短路径，保留完整数据流");
        subPathLabel.setFont(subPathLabel.getFont().deriveFont(Font.PLAIN, 10f));
        subPathLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        gbc.gridx = 1; gbc.gridy = 1; gbc.insets = JBUI.insets(2, 10, 2, 0);
        panel.add(subPathLabel, gbc);
        
        // Sink位置去重选项
        enableSinkLocationDeduplicationCheckBox = new JBCheckBox("Sink位置去重", true);
        enableSinkLocationDeduplicationCheckBox.setToolTipText("基于sink的文件位置进行精确去重，避免相同位置的重复报告");
        gbc.gridx = 0; gbc.gridy = 2; gbc.insets = JBUI.insets(2, 0);
        panel.add(enableSinkLocationDeduplicationCheckBox, gbc);
        
        JBLabel locationLabel = new JBLabel("基于文件位置去重，避免重复报告");
        locationLabel.setFont(locationLabel.getFont().deriveFont(Font.PLAIN, 10f));
        locationLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        gbc.gridx = 1; gbc.gridy = 2; gbc.insets = JBUI.insets(2, 10, 2, 0);
        panel.add(locationLabel, gbc);
        
        // 上下文过滤选项
        enableContextFilteringCheckBox = new JBCheckBox("上下文过滤", true);
        enableContextFilteringCheckBox.setToolTipText("应用现有的上下文过滤器，移除特定上下文中的误报");
        gbc.gridx = 0; gbc.gridy = 3; gbc.insets = JBUI.insets(2, 0);
        panel.add(enableContextFilteringCheckBox, gbc);
        
        JBLabel contextLabel = new JBLabel("应用上下文过滤器，减少误报");
        contextLabel.setFont(contextLabel.getFont().deriveFont(Font.PLAIN, 10f));
        contextLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        gbc.gridx = 1; gbc.gridy = 3; gbc.insets = JBUI.insets(2, 10, 2, 0);
        panel.add(contextLabel, gbc);
        
        // 重置按钮
        JButton resetButton = new JButton("重置为默认");
        resetButton.setFont(resetButton.getFont().deriveFont(Font.PLAIN, 11f));
        resetButton.addActionListener(e -> resetOptimizationOptions());
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.insets = JBUI.insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(resetButton, gbc);
        
        return panel;
    }
    
    /**
     * 重置优化选项为默认值
     */
    private void resetOptimizationOptions() {
        enableSubPathDeduplicationCheckBox.setSelected(true);
        enableSinkLocationDeduplicationCheckBox.setSelected(true);
        enableContextFilteringCheckBox.setSelected(true);
    }
    
    /**
     * 获取优化配置
     */
    public com.flowspot.intellij.model.OptimizationConfig getOptimizationConfig() {
        return new com.flowspot.intellij.model.OptimizationConfig(
            enableSubPathDeduplicationCheckBox.isSelected(),
            enableSinkLocationDeduplicationCheckBox.isSelected(),
            enableContextFilteringCheckBox.isSelected()
        );
    }
    
    @Override
    protected void dispose() {
        // 清理配置管理器资源
        if (configManager != null) {
            configManager.dispose();
        }
        super.dispose();
    }
}
