/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.gui;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.flowspot.intellij.model.FlowSpotVulnerability;
import com.flowspot.intellij.service.FlowSpotVulnerabilitySelectionPublisher;
import omni.flowspot.core.FlowSpotBugInstance;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * FlowSpot 数据流面板
 * 使用紧凑型组件显示数据流路径
 */
public class FlowSpotDataFlowPanel extends JPanel {
    private final Project project;
    private MessageBusConnection messageBusConnection;
    private CompactDataFlowPanel compactFlowPanel;
    
    public FlowSpotDataFlowPanel(@NotNull Project project) {
        this.project = project;
        initializeUI();
        setupMessageBusSubscription();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        compactFlowPanel = new CompactDataFlowPanel(project);
        add(compactFlowPanel, BorderLayout.CENTER);
    }
    
    private void setupMessageBusSubscription() {
        messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(FlowSpotVulnerabilitySelectionPublisher.TOPIC, 
            new FlowSpotVulnerabilitySelectionPublisher() {
                public void onVulnerabilitySelected(FlowSpotVulnerability vulnerability) {
                    updateDataFlowDisplay(vulnerability);
                }
                
                public void onVulnerabilityCollectionChanged(
                    com.flowspot.intellij.model.FlowSpotVulnerabilityCollection collection) {
                    // 处理集合变化
                }
            });
    }
    
    private void updateDataFlowDisplay(FlowSpotVulnerability vulnerability) {
        SwingUtilities.invokeLater(() -> {
            if (vulnerability == null) {
                compactFlowPanel.clearContent();
                return;
            }
            
            // 转换为FlowSpotBugInstance
            FlowSpotBugInstance bugInstance = convertToBugInstance(vulnerability);
            if (bugInstance != null) {
                compactFlowPanel.setVulnerability(bugInstance);
            } else {
                compactFlowPanel.clearContent();
            }
        });
    }
    
    private FlowSpotBugInstance convertToBugInstance(FlowSpotVulnerability vulnerability) {
        if (vulnerability != null && vulnerability.getOriginalBugInstance() != null) {
            return vulnerability.getOriginalBugInstance();
        }
        return null;
    }
    
    /**
     * 获取当前漏洞集合（为了兼容原有的FlowSpotDataFlowNodeComponent）
     */
    public com.flowspot.intellij.model.FlowSpotVulnerabilityCollection getCurrentCollection() {
        // TODO: 在实际集成时，这里应该返回当前的漏洞集合
        // 可以通过消息总线或者其他方式获取
        return null;
    }
    
    public void dispose() {
        if (messageBusConnection != null) {
            messageBusConnection.disconnect();
        }
    }
}
