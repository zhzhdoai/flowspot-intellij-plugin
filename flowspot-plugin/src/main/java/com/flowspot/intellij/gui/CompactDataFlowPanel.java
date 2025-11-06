/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.gui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import omni.flowspot.annotations.FlowSpotSourceLineAnnotation;
import omni.flowspot.core.FlowSpotBugInstance;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 紧凑型数据流可视化面板
 * 使用统一样式的紧凑节点组件显示数据流路径
 */
public class CompactDataFlowPanel extends JPanel {
    
    private final Project project;
    private JPanel flowContainer;
    private JScrollPane scrollPane;
    
    public CompactDataFlowPanel(@NotNull Project project) {
        this.project = project;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        
        // 创建流容器，使用居中布局
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centerWrapper.setBackground(Color.BLACK);
        
        flowContainer = new JPanel();
        flowContainer.setLayout(new BoxLayout(flowContainer, BoxLayout.Y_AXIS));
        flowContainer.setBackground(Color.BLACK);
        
        centerWrapper.add(flowContainer);
        
        // 创建滚动面板
        scrollPane = new JBScrollPane(centerWrapper);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 添加标题
        JLabel titleLabel = new JLabel("Data Flow Path", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(titleLabel, BorderLayout.NORTH);
    }
    
    /**
     * 设置要显示的漏洞实例
     */
    public void setVulnerability(@NotNull FlowSpotBugInstance vulnerability) {
        SwingUtilities.invokeLater(() -> {
            flowContainer.removeAll();
            
            Collection<FlowSpotSourceLineAnnotation> annotationCollection = vulnerability.getAnnotations();
            List<FlowSpotSourceLineAnnotation> annotations = new ArrayList<>(annotationCollection);
            if (annotations == null || annotations.isEmpty()) {
                showEmptyState();
                return;
            }
            
            // 添加流路径标题
            addFlowPathHeader(vulnerability);
            
            // 创建节点组件（只对Identifier节点使用紧凑样式）
            for (int i = 0; i < annotations.size(); i++) {
                FlowSpotSourceLineAnnotation annotation = annotations.get(i);
                boolean isLastNode = (i == annotations.size() - 1);
                boolean isIdentifier = isIdentifierAnnotation(annotation);
                
                if (isIdentifier) {
                    // Identifier节点使用紧凑组件
                    CompactDataFlowNodeComponent nodeComponent = new CompactDataFlowNodeComponent(
                        project, annotation, i, isLastNode
                    );
                    flowContainer.add(nodeComponent);
                    
                    // 添加间距（除了最后一个节点）
                    if (!isLastNode) {
                        flowContainer.add(Box.createVerticalStrut(2));
                    }
                }
            }
            
            // 添加底部间距
            flowContainer.add(Box.createVerticalGlue());
            
            flowContainer.revalidate();
            flowContainer.repaint();
            
            // 滚动到顶部
            SwingUtilities.invokeLater(() -> {
                scrollPane.getVerticalScrollBar().setValue(0);
            });
        });
    }
    
    private void addFlowPathHeader(FlowSpotBugInstance vulnerability) {
//        JPanel headerPanel = new JPanel(new BorderLayout());
//        headerPanel.setBackground(Color.WHITE);
//        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
//        // 限制headerPanel的高度
//        headerPanel.setPreferredSize(new Dimension(0, 40));
//        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // 漏洞类型标签
//        JLabel typeLabel = new JLabel(vulnerability.getType());
//        typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD, 12f));
//        typeLabel.setForeground(new Color(244, 67, 54)); // 红色
        
        // 路径长度信息
        int pathLength = vulnerability.getAnnotations().size();
//        JLabel pathInfo = new JLabel(String.format("Path: %d steps", pathLength));
//        pathInfo.setFont(pathInfo.getFont().deriveFont(Font.PLAIN, 10f));
//        pathInfo.setForeground(Color.GRAY);
        
//        headerPanel.add(typeLabel, BorderLayout.WEST);
//        headerPanel.add(pathInfo, BorderLayout.EAST);
        
        // 添加分隔线
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.LIGHT_GRAY);
        
//        flowContainer.add(headerPanel);
        flowContainer.add(separator);
        flowContainer.add(Box.createVerticalStrut(8));
    }
    
    /**
     * 判断是否为Identifier节点
     */
    private boolean isIdentifierAnnotation(FlowSpotSourceLineAnnotation annotation) {
        return "Identifier".equals(annotation.getNodeType());
    }
    
    private void showEmptyState() {
        flowContainer.removeAll();
        
        JLabel emptyLabel = new JLabel("No data flow path available", SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.ITALIC));
        
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(Color.WHITE);
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);
        
        flowContainer.add(emptyPanel);
        flowContainer.revalidate();
        flowContainer.repaint();
    }
    
    /**
     * 清空显示内容
     */
    public void clearContent() {
        SwingUtilities.invokeLater(() -> {
            flowContainer.removeAll();
            flowContainer.revalidate();
            flowContainer.repaint();
        });
    }
}
