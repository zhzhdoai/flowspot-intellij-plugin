/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.gui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import omni.flowspot.annotations.FlowSpotSourceLineAnnotation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * 紧凑型数据流节点组件
 * 统一样式，支持箭头连接和紧凑布局
 */
public class CompactDataFlowNodeComponent extends JPanel {
    
    private final Project project;
    private final FlowSpotSourceLineAnnotation annotation;
    private final int nodeIndex;
    private final boolean isLastNode;
    
    // 统一的节点样式
    private static final Color NODE_COLOR = new Color(33, 150, 243); // 统一蓝色
    private static final String NODE_ICON = "⚪"; // 统一图标
    
    // 尺寸常量
    private static final int NODE_WIDTH = 200;
    private static final int NODE_HEIGHT = 45;
    private static final int ARROW_HEIGHT = 20;
    private static final int CORNER_RADIUS = 8;
    
    public CompactDataFlowNodeComponent(@NotNull Project project,
                                     @NotNull FlowSpotSourceLineAnnotation annotation,
                                     int nodeIndex,
                                     boolean isLastNode) {
        this.project = project;
        this.annotation = annotation;
        this.nodeIndex = nodeIndex;
        this.isLastNode = isLastNode;
        
        initializeComponent();
        setupEventHandlers();
    }
    
    private void initializeComponent() {
        setLayout(null); // 使用绝对布局以精确控制位置
        setPreferredSize(new Dimension(NODE_WIDTH, NODE_HEIGHT + (isLastNode ? 0 : ARROW_HEIGHT)));
        setMaximumSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        setOpaque(false); // 透明背景，自定义绘制
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    private void setupEventHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigateToCode();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                repaint(); // 触发重绘以显示悬停效果
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                repaint(); // 触发重绘以移除悬停效果
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        try {
            drawNode(g2d);
            if (!isLastNode) {
                drawArrow(g2d);
            }
        } finally {
            g2d.dispose();
        }
    }
    
    private void drawNode(Graphics2D g2d) {
        // 检查鼠标悬停状态
        boolean isHovered = getMousePosition() != null;
        
        // 节点主体区域
        Rectangle nodeRect = new Rectangle(0, 0, NODE_WIDTH, NODE_HEIGHT);
        
        // 绘制节点背景
        Color bgColor = isHovered ? 
            brightenColor(NODE_COLOR, 0.2f) : 
            NODE_COLOR.brighter();
        
        g2d.setColor(bgColor);
        g2d.fillRoundRect(nodeRect.x, nodeRect.y, nodeRect.width, nodeRect.height, 
                         CORNER_RADIUS, CORNER_RADIUS);
        
        // 绘制节点边框
        g2d.setColor(NODE_COLOR.darker());
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(nodeRect.x, nodeRect.y, nodeRect.width, nodeRect.height,
                         CORNER_RADIUS, CORNER_RADIUS);
        
        // 绘制节点内容
        drawNodeContent(g2d, nodeRect);
        
        // 绘制节点序号
//        drawNodeIndex(g2d, nodeRect);
    }
    
    private void drawNodeContent(Graphics2D g2d, Rectangle nodeRect) {
        // 绘制节点图标
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        // 绘制代码信息（居中）
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2d.setColor(Color.BLACK);
        
        String codeText = getDisplayText();
        FontMetrics codeFm = g2d.getFontMetrics();
        // 计算居中位置（在节点内居中）
        int codeX = nodeRect.x + (nodeRect.width - codeFm.stringWidth(codeText)) / 2;
        int codeY = nodeRect.y + (nodeRect.height - codeFm.getHeight()) / 2 + codeFm.getAscent() - 5;
        g2d.drawString(codeText, codeX, codeY);
        
        // 绘制位置信息（居中）
        String locationText = getLocationText();
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2d.setColor(Color.DARK_GRAY);
        
        FontMetrics locFm = g2d.getFontMetrics();
        // 计算居中位置
        int locX = nodeRect.x + (nodeRect.width - locFm.stringWidth(locationText)) / 2;
        int locY = nodeRect.y + nodeRect.height - 6;
        g2d.drawString(locationText, locX, locY);
    }
    
    private void drawNodeIndex(Graphics2D g2d, Rectangle nodeRect) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        
        String indexText = String.valueOf(nodeIndex + 1);
        FontMetrics fm = g2d.getFontMetrics();
        int indexX = nodeRect.x + nodeRect.width - fm.stringWidth(indexText) - 8;
        int indexY = nodeRect.y + fm.getAscent() + 4;
        
        // 绘制圆形背景
        int circleSize = 16;
        g2d.setColor(NODE_COLOR.darker());
        g2d.fillOval(indexX - 4, indexY - fm.getAscent() - 2, circleSize, circleSize);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString(indexText, indexX, indexY);
    }
    
    private void drawArrow(Graphics2D g2d) {
        // 绘制向下的箭头连接线
        int centerX = NODE_WIDTH / 2;
        int startY = NODE_HEIGHT;
        int endY = NODE_HEIGHT + ARROW_HEIGHT;
        
        // 绘制箭头线
        g2d.setColor(JBColor.GRAY);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(centerX, startY, centerX, endY - 8);
        
        // 绘制箭头头部
        Path2D arrowHead = new Path2D.Float();
        arrowHead.moveTo(centerX, endY);
        arrowHead.lineTo(centerX - 6, endY - 8);
        arrowHead.lineTo(centerX + 6, endY - 8);
        arrowHead.closePath();
        
        g2d.fill(arrowHead);
    }
    
    private String getDisplayText() {
        if (annotation.getIdentifierName() != null) {
            return annotation.getIdentifierName();
        }
        return "Code";
    }
    
    private String getLocationText() {
        return String.format("Method %s", annotation.getMethodName());
    }
    
    private Color brightenColor(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() * (1 + factor)));
        int g = Math.min(255, (int) (color.getGreen() * (1 + factor)));
        int b = Math.min(255, (int) (color.getBlue() * (1 + factor)));
        return new Color(r, g, b);
    }

    /**
     * 导航到代码位置 - 复用原有的导航逻辑
     */
    private void navigateToCode() {
        // 复用原有FlowSpotDataFlowNodeComponent的导航逻辑
        // 创建一个临时的原有组件来处理导航
        FlowSpotDataFlowNodeComponent originalComponent = 
            new FlowSpotDataFlowNodeComponent(project, annotation, false);
        
        // 直接调用原有组件的导航方法
        try {
            java.lang.reflect.Method navigateMethod = 
                FlowSpotDataFlowNodeComponent.class.getDeclaredMethod("navigateToCode");
            navigateMethod.setAccessible(true);
            navigateMethod.invoke(originalComponent);
        } catch (Exception e) {
            // 如果反射失败，显示错误信息
            System.err.println("Failed to navigate to code: " + e.getMessage());
        }
    }
}
