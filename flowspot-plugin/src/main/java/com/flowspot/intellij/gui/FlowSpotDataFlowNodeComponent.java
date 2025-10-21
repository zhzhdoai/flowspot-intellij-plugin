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
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import omni.flowspot.annotations.FlowSpotSourceLineAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * FlowSpot 数据流节点可视化组件
 * 显示单个数据流节点，支持点击跳转到代码
 */
public class FlowSpotDataFlowNodeComponent extends JPanel {
    
    private final Project project;
    private final FlowSpotSourceLineAnnotation annotation;
    private final boolean isIdentifier;
    private JLabel nodeTypeLabel;
    private JLabel codeLabel;
    private JLabel locationLabel;
    
    // 颜色定义
    private static final Color IDENTIFIER_COLOR = new Color(173, 216, 230); // 浅蓝色
    private static final Color IDENTIFIER_BORDER_COLOR = new Color(100, 149, 237); // 深蓝色
    private static final Color HOVER_COLOR = new Color(135, 206, 250); // 亮蓝色
    private static final Color DEFAULT_COLOR = new Color(240, 240, 240); // 浅灰色
    
    public FlowSpotDataFlowNodeComponent(@NotNull Project project, 
                                       @NotNull FlowSpotSourceLineAnnotation annotation,
                                       boolean isIdentifier) {
        this.project = project;
        this.annotation = annotation;
        this.isIdentifier = isIdentifier;
        
        initializeComponent();
        setupEventHandlers();
    }
    
    /**
     * 初始化组件
     */
    private void initializeComponent() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        // 设置固定宽度，不占满左右
        setPreferredSize(new Dimension(180, 70));
        setMaximumSize(new Dimension(180, 70));
        setMinimumSize(new Dimension(180, 70));
        
        // 设置背景色和边框
        if (isIdentifier) {
            setBackground(IDENTIFIER_COLOR);
            setBorder(createIdentifierBorder());
        } else {
            setBackground(DEFAULT_COLOR);
            setBorder(createDefaultBorder());
        }
        
        // 创建标签
        createLabels();
        
        // 设置鼠标样式
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // 添加内边距 - 缩小边框
        setBorder(BorderFactory.createCompoundBorder(
            getBorder(),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
    }
    
    /**
     * 创建标识符节点的特殊边框
     */
    private Border createIdentifierBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(IDENTIFIER_BORDER_COLOR, 1),
            BorderFactory.createLineBorder(Color.WHITE, 1)
        );
    }
    
    /**
     * 创建默认节点的边框
     */
    private Border createDefaultBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createLineBorder(Color.WHITE, 1)
        );
    }
    
    /**
     * 创建标签组件
     */
    private void createLabels() {
        // 节点类型标签 - 去掉[IDENTIFIER]显示，只显示星号
        nodeTypeLabel = new JLabel();
        if (isIdentifier) {
            nodeTypeLabel.setFont(nodeTypeLabel.getFont().deriveFont(Font.BOLD, 14f));
            nodeTypeLabel.setForeground(new Color(70, 130, 180)); // 钢蓝色
            nodeTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(nodeTypeLabel);

            codeLabel = new JLabel();
            String code = annotation.getIdentifierName();
            codeLabel.setText(code != null ? code : "N/A");
            codeLabel.setFont(codeLabel.getFont().deriveFont(Font.PLAIN, 10f));
            codeLabel.setForeground(Color.BLACK);
            codeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            codeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(codeLabel);

            // 位置信息标签
            locationLabel = new JLabel();
            String methodName = annotation.getMethodName();
            locationLabel.setText(annotation.getClassName()+"#"+methodName);
            locationLabel.setFont(locationLabel.getFont().deriveFont(Font.ITALIC, 9f));
            locationLabel.setForeground(Color.GRAY);
            locationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            locationLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(locationLabel);
        }

    }
    
    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 鼠标点击事件
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    navigateToCode();
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isIdentifier) {
                    setBackground(HOVER_COLOR);
                } else {
                    setBackground(new Color(220, 220, 220)); // 浅灰色悬停
                }
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (isIdentifier) {
                    setBackground(IDENTIFIER_COLOR);
                } else {
                    setBackground(DEFAULT_COLOR);
                }
                repaint();
            }
        });
    }
    
    /**
     * 导航到代码位置 - 复用Data Flow的精确高亮逻辑
     */
    private void navigateToCode() {
        String sourceFile = annotation.getSourceFile();
        if (sourceFile == null) {
            return;
        }
        
        // 直接使用被分析目录拼接filename
        String fullPath = constructFullFilePath(sourceFile);
        
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(fullPath);
            if (virtualFile != null) {
                int lineNumber = annotation.getStartLine();
                // 使用Data Flow的精确定位和高亮方法
                openFileWithPreciseLocation(virtualFile, annotation, lineNumber);
            } else {
                // 显示错误消息
                JOptionPane.showMessageDialog(this,
                    "Cannot find source file: " + sourceFile + "\n" +
                    "Full path: " + fullPath,
                    "Navigation Error",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
    }
    
    /**
     * 打开文件并实现精确的列定位和代码高亮 - 复用Data Flow的实现
     */
    private void openFileWithPreciseLocation(@NotNull VirtualFile virtualFile,
                                           @NotNull FlowSpotSourceLineAnnotation annotation,
                                           int lineNumber) {
        ApplicationManager.getApplication().invokeLater(() -> {
            // 打开文件
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, virtualFile, lineNumber - 1, 0);
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);

            if (editor != null) {
                // 实现精确的列定位和高亮
                highlightCodeRange(editor, annotation, lineNumber);
            }
        });
    }

    /**
     * 在编辑器中高亮指定的代码范围 - 复用Data Flow的实现
     */
    private void highlightCodeRange(@NotNull Editor editor,
                                  @NotNull FlowSpotSourceLineAnnotation annotation,
                                  int lineNumber) {
        Document document = editor.getDocument();

        // 确保行号有效
        if (lineNumber <= 0 || lineNumber > document.getLineCount()) {
            return;
        }

        int lineStartOffset = document.getLineStartOffset(lineNumber - 1);
        int lineEndOffset = document.getLineEndOffset(lineNumber - 1);

        // 计算精确的列位置
        int startOffset = lineStartOffset;
        int endOffset = lineEndOffset;

        // 直接使用 startBytecode 和 endBytecode 作为准确的列位置
        if (annotation.getStartBytecode() >= 0 && annotation.getEndBytecode() >= 0) {
            // startBytecode 和 endBytecode 就是准确的列偏移
            int startColumn = annotation.getStartBytecode() -1;
            int endColumn = annotation.getEndBytecode() -1 ;
            
            // 确保列位置在行范围内
            int lineLength = lineEndOffset - lineStartOffset;
            if (startColumn >= 0 && startColumn <= lineLength) {
                startOffset = lineStartOffset + startColumn-1;
            }
            if (endColumn >= 0 && endColumn <= lineLength && endColumn > startColumn) {
                endOffset = lineStartOffset + endColumn;
            } else {
                // 如果 endColumn 无效，使用 startColumn + 合理长度
                endOffset = Math.min(startOffset + 10, lineEndOffset);
            }
        }

        // 设置光标位置
        CaretModel caretModel = editor.getCaretModel();
        caretModel.moveToOffset(startOffset+1);

        // 选中代码范围
        SelectionModel selectionModel = editor.getSelectionModel();
        selectionModel.setSelection(startOffset+1, endOffset+1);

        // 添加高亮
        addHighlight(editor, startOffset+1, endOffset+1);

        // 滚动到可见区域
        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }

    /**
     * 添加代码高亮 - 使用统一的高亮服务
     */
    private void addHighlight(@NotNull Editor editor, int startOffset, int endOffset) {
        com.flowspot.intellij.service.FlowSpotHighlightService.getInstance()
            .addHighlight(editor, startOffset, endOffset);
    }
    
    /**
     * 构建完整文件路径 - 直接使用被分析目录拼接filename
     */
    private String constructFullFilePath(String sourceFile) {

        
        // 如果已经是绝对路径，直接返回
        if (sourceFile.startsWith("/") || sourceFile.matches("^[A-Za-z]:.*")) {
            return sourceFile;
        }

        // 获取被分析目录路径
        String analysisBasePath = determineAnalysisBasePath();
        if (analysisBasePath == null) {
            return sourceFile;
        }

        // 直接使用被分析目录拼接源文件
        String fullPath = analysisBasePath + "/" + sourceFile;

        // 规范化路径
        try {
            String normalizedPath = java.nio.file.Paths.get(fullPath).normalize().toString();
            return normalizedPath;
        } catch (Exception e) {
            return fullPath;
        }
    }
    
    /**
     * 确定分析的基础路径
     * 优先使用当前漏洞集合中的分析基础路径，如果无法确定则使用项目根路径
     */
    private String determineAnalysisBasePath() {
        // 从当前漏洞集合中获取分析基础路径
        com.flowspot.intellij.model.FlowSpotVulnerabilityCollection currentCollection = getCurrentVulnerabilityCollection();
        if (currentCollection != null && currentCollection.getAnalysisBasePath() != null) {
            String analysisPath = currentCollection.getAnalysisBasePath();
            return analysisPath;
        }

        // 后备方案1：尝试从主工具窗口获取collection
        currentCollection = getCollectionFromMainToolWindow();
        if (currentCollection != null && currentCollection.getAnalysisBasePath() != null) {
            String analysisPath = currentCollection.getAnalysisBasePath();
            return analysisPath;
        }

        // 后备方案2：使用项目根路径
        String projectBasePath = project.getBasePath();
        return projectBasePath;
    }

    /**
     * 获取当前的漏洞集合
     */
    private com.flowspot.intellij.model.FlowSpotVulnerabilityCollection getCurrentVulnerabilityCollection() {
        // 通过父组件获取当前的漏洞集合
        Component parent = getParent();
        while (parent != null) {
            if (parent instanceof FlowSpotDataFlowPanel) {
                com.flowspot.intellij.model.FlowSpotVulnerabilityCollection collection = 
                    ((FlowSpotDataFlowPanel) parent).getCurrentCollection();
                return collection;
            }
            parent = parent.getParent();
        }
        return null;
    }
    
    /**
     * 从主工具窗口获取当前的漏洞集合
     */
    @Nullable
    private com.flowspot.intellij.model.FlowSpotVulnerabilityCollection getCollectionFromMainToolWindow() {
        try {
            // 通过工具窗口管理器获取主FlowSpot工具窗口
            com.intellij.openapi.wm.ToolWindowManager toolWindowManager = 
                com.intellij.openapi.wm.ToolWindowManager.getInstance(project);
            com.intellij.openapi.wm.ToolWindow toolWindow = toolWindowManager.getToolWindow("FlowSpot");
            
            if (toolWindow != null && toolWindow.getContentManager().getContentCount() > 0) {
                javax.swing.JComponent component = toolWindow.getContentManager().getContent(0).getComponent();
                if (component instanceof FlowSpotToolWindowPanel) {
                    return ((FlowSpotToolWindowPanel) component).getCurrentCollection();
                }
            }
        } catch (Exception e) {
            System.out.println("FlowSpotDataFlowNodeComponent: 获取主工具窗口collection失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取注解对象
     */
    public FlowSpotSourceLineAnnotation getAnnotation() {
        return annotation;
    }
    
    /**
     * 是否为标识符节点
     */
    public boolean isIdentifier() {
        return isIdentifier;
    }
}
