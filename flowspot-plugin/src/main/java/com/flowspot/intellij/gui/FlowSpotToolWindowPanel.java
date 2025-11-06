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
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.flowspot.intellij.model.FlowSpotVulnerabilityCollection;
import com.flowspot.intellij.model.FlowSpotVulnerability;
import com.flowspot.intellij.service.FlowSpotResultsPublisher;
import com.flowspot.intellij.service.FlowSpotVulnerabilitySelectionPublisher;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.*;

/**
 * FlowSpot 工具窗口主面板
 * 显示漏洞分析结果和相关信息
 */
public class FlowSpotToolWindowPanel extends JPanel {
    
    private final Project project;
    private MessageBusConnection messageBusConnection;
    private JLabel statusLabel;
    private FlowSpotVulnerabilityTreePanel treePanel;
    private FlowSpotVulnerabilityDetailsPanel detailsPanel;
    private FlowSpotVulnerabilityCollection currentCollection;
    
    public FlowSpotToolWindowPanel(@NotNull Project project) {
        this.project = project;
        initializeUI();
        subscribeToResults();
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 创建状态标签
        statusLabel = new JLabel("FlowSpot - Ready for analysis");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.NORTH);
        
        // 创建漏洞树面板
        treePanel = new FlowSpotVulnerabilityTreePanel(project);
        
        // 创建详情面板
        detailsPanel = new FlowSpotVulnerabilityDetailsPanel(project);
        
        // 设置过滤规则变化监听器，当添加过滤规则时刷新树面板
        detailsPanel.setFilterRuleChangeListener(() -> {
            System.out.println("FlowSpotToolWindowPanel: 接收到过滤规则变化通知");
            if (currentCollection != null) {
                System.out.println("FlowSpotToolWindowPanel: 当前有漏洞集合，刷新树面板");
                treePanel.refreshWithCurrentFilters();
            } else {
                System.out.println("FlowSpotToolWindowPanel: 当前没有漏洞集合，无法刷新");
            }
        });
        
        // 创建主要的水平分割面板（左右分割）
        // 左侧：漏洞树（FlowSpot Results）
        // 右侧：详情面板（Data Flow）
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // 左侧：漏洞树面板
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("FlowSpot Results"));
        leftPanel.add(treePanel, BorderLayout.CENTER);
        
        // 右侧：详情面板
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Data Flow Details"));
        rightPanel.add(detailsPanel, BorderLayout.CENTER);
        
        // 设置分割面板
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        mainSplitPane.setDividerLocation(400);
        mainSplitPane.setResizeWeight(0.5);
        
        add(mainSplitPane, BorderLayout.CENTER);
        
        // 设置树选择监听器
        setupTreeSelectionListener();
        
        // 创建底部信息面板
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("FlowSpot Security Vulnerability Analyzer");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 10f));
        infoPanel.add(infoLabel);
        add(infoPanel, BorderLayout.SOUTH);
    }
    
    
    /**
     * 设置树选择监听器
     */
    private void setupTreeSelectionListener() {
        treePanel.addVulnerabilitySelectionListener(new FlowSpotVulnerabilityTreePanel.VulnerabilitySelectionListener() {
            @Override
            public void onVulnerabilitySelected(@org.jetbrains.annotations.Nullable FlowSpotVulnerability vulnerability) {
                detailsPanel.setVulnerability(vulnerability);
                
                // 发布漏洞选择事件，让数据流面板同步更新
                MessageBus messageBus = project.getMessageBus();
                FlowSpotVulnerabilitySelectionPublisher publisher = messageBus.syncPublisher(FlowSpotVulnerabilitySelectionPublisher.TOPIC);
                publisher.onVulnerabilitySelected(vulnerability);
            }
        });
    }
    
    /**
     * 订阅分析结果
     */
    private void subscribeToResults() {
        MessageBus messageBus = project.getMessageBus();
        messageBusConnection = messageBus.connect();
        
        messageBusConnection.subscribe(FlowSpotResultsPublisher.TOPIC, new FlowSpotResultsPublisher() {
            @Override
            public void onFlowSpotResultsAvailable(@NotNull FlowSpotVulnerabilityCollection collection) {
                SwingUtilities.invokeLater(() -> updateResults(collection));
            }
            
            @Override
            public void onAnalysisStarted(@NotNull String projectName) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("FlowSpot - Analysis in progress...");
                    treePanel.setVulnerabilityCollection(null);
                    detailsPanel.setVulnerability(null);
                });
            }
            
            @Override
            public void onAnalysisCompleted(@NotNull String projectName, boolean success, @NotNull String errorMessage) {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        statusLabel.setText("FlowSpot - Analysis completed");
                    } else {
                        statusLabel.setText("FlowSpot - Analysis failed: " + errorMessage);
                        treePanel.setVulnerabilityCollection(null);
                        detailsPanel.setVulnerability(null);
                    }
                });
            }
            
            @Override
            public void onAnalysisCancelled(@NotNull String projectName) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("FlowSpot - Analysis cancelled");
                    treePanel.setVulnerabilityCollection(null);
                    detailsPanel.setVulnerability(null);
                });
            }
        });
    }
    
    /**
     * 更新分析结果显示
     */
    private void updateResults(@NotNull FlowSpotVulnerabilityCollection collection) {
        this.currentCollection = collection;
        
        if (collection.isEmpty()) {
            statusLabel.setText("FlowSpot - No vulnerabilities found");
        } else {
            statusLabel.setText("FlowSpot - Found " + collection.getTotalCount() + " vulnerabilities");
        }
        
        // 更新树面板
        treePanel.setVulnerabilityCollection(collection);
        
        // 清空详情面板
        detailsPanel.setVulnerability(null);
    }
    
    
    /**
     * 获取当前的漏洞集合
     */
    public FlowSpotVulnerabilityCollection getCurrentCollection() {
        return currentCollection;
    }
    
    /**
     * 清理资源
     */
    public void dispose() {
        if (messageBusConnection != null) {
            messageBusConnection.disconnect();
        }
    }
}
