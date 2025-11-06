package com.flowspot.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * FlowSpot 统一高亮管理服务
 * 提供全局的代码高亮管理，确保同一时间只有一个高亮存在
 */
@Service
public final class FlowSpotHighlightService {
    
    // 当前高亮信息
    private RangeHighlighter currentHighlighter = null;
    private Editor currentEditor = null;
    
    /**
     * 获取服务实例
     */
    public static FlowSpotHighlightService getInstance() {
        return ApplicationManager.getApplication().getService(FlowSpotHighlightService.class);
    }
    
    /**
     * 添加代码高亮，自动移除之前的高亮
     * 
     * @param editor 编辑器
     * @param startOffset 开始偏移量
     * @param endOffset 结束偏移量
     */
    public void addHighlight(@NotNull Editor editor, int startOffset, int endOffset) {
        // 移除之前的高亮
        removeCurrentHighlight();
        
        MarkupModel markupModel = editor.getMarkupModel();
        
        // 创建高亮属性
        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(JBColor.YELLOW);
        attributes.setForegroundColor(JBColor.BLACK);
        
        // 添加高亮
        RangeHighlighter highlighter = markupModel.addRangeHighlighter(
            startOffset, endOffset,
            HighlighterLayer.SELECTION,
            attributes,
            com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
        );
        
        // 保存当前高亮信息
        currentHighlighter = highlighter;
        currentEditor = editor;
    }
    
    /**
     * 移除当前的高亮
     */
    public void removeCurrentHighlight() {
        if (currentHighlighter != null && currentEditor != null && currentHighlighter.isValid()) {
            currentEditor.getMarkupModel().removeHighlighter(currentHighlighter);
        }
        currentHighlighter = null;
        currentEditor = null;
    }
    
    /**
     * 检查是否有当前高亮
     */
    public boolean hasCurrentHighlight() {
        return currentHighlighter != null && currentEditor != null && currentHighlighter.isValid();
    }
    
    /**
     * 获取当前高亮的编辑器
     */
    @Nullable
    public Editor getCurrentEditor() {
        return hasCurrentHighlight() ? currentEditor : null;
    }
}
