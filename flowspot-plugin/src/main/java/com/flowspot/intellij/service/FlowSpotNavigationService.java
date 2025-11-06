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
package com.flowspot.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.ui.JBColor;
import com.flowspot.intellij.model.FlowSpotLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;

/**
 * FlowSpot 代码导航服务
 * 负责将漏洞位置导航到对应的源代码
 */
public class FlowSpotNavigationService {
    
    private final Project project;
    
    
    public FlowSpotNavigationService(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * 导航到指定位置
     * 
     * @param location 漏洞位置信息
     * @return 导航是否成功
     */
    public boolean navigateToLocation(@NotNull FlowSpotLocation location) {
        // 在后台线程中查找文件
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            VirtualFile file = ApplicationManager.getApplication().runReadAction((com.intellij.openapi.util.Computable<VirtualFile>) () -> findFile(location));
            
            if (file != null) {
                // 在EDT上打开文件
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        // 打开文件
                        OpenFileDescriptor descriptor = new OpenFileDescriptor(project, file);
                        Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                        
                        if (editor != null && location.hasLineInfo()) {
                            // 实现精确的列定位和代码高亮
                            highlightLocationInEditor(editor, location);
                        }
                    } catch (Exception e) {
                        // 忽略异常
                    }
                });
            }
        });
        
        return true; // 异步操作，总是返回true
    }
    
    /**
     * 在编辑器中高亮指定的位置
     */
    private void highlightLocationInEditor(@NotNull Editor editor, @NotNull FlowSpotLocation location) {
        Document document = editor.getDocument();
        
        // 导航到具体行号
        int line = location.getStartLine() - 1; // 转换为0基索引
        if (line < 0 || line >= document.getLineCount()) {
            return;
        }
        
        int lineStartOffset = document.getLineStartOffset(line);
        int lineEndOffset = document.getLineEndOffset(line);
        
        // 计算精确的列位置
        int startOffset = lineStartOffset;
        int endOffset = lineEndOffset;
        
        // 如果有列信息，使用精确的列定位
        if (location.hasColumnInfo()) {
            int startColumn = location.getStartColumn() - 1; // 转换为0基索引
            int endColumn = location.getEndColumn() - 1;
            
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
        // 请求焦点
        editor.getContentComponent().requestFocus();
    }
    
    /**
     * 添加代码高亮 - 使用统一的高亮服务
     */
    private void addHighlight(@NotNull Editor editor, int startOffset, int endOffset) {
        FlowSpotHighlightService.getInstance().addHighlight(editor, startOffset, endOffset);
    }
    
    /**
     * 查找文件
     */
    @Nullable
    private VirtualFile findFile(@NotNull FlowSpotLocation location) {
        String fileName = location.getFileName();
        String className = location.getClassName();
        
        // 策略1: 通过文件名搜索
        VirtualFile file = findByFileName(fileName);
        if (file != null) {
            return file;
        }
        
        // 策略2: 通过类名搜索
        if (className != null && !className.isEmpty()) {
            file = findByClassName(className);
            if (file != null) {
                return file;
            }
        }
        
        // 策略3: 通过本地文件系统
        file = findByLocalFileSystem(fileName);
        if (file != null) {
            return file;
        }
        
        return null;
    }
    
    /**
     * 通过文件名查找文件
     */
    @Nullable
    private VirtualFile findByFileName(@NotNull String fileName) {
        try {
            // 获取简单文件名（去掉路径）
            String simpleFileName = new File(fileName).getName();
            
            // 在项目范围内搜索
            Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(
                project, simpleFileName, GlobalSearchScope.projectScope(project));
            
            if (!files.isEmpty()) {
                // 优先选择完全匹配的文件
                for (VirtualFile file : files) {
                    if (file.getPath().endsWith(fileName)) {
                        return file;
                    }
                }
                
                // 如果没有完全匹配，返回第一个
                return files.iterator().next();
            }
            
            // 尝试在所有范围内搜索
            files = FilenameIndex.getVirtualFilesByName(
                project, simpleFileName, GlobalSearchScope.allScope(project));
            
            if (!files.isEmpty()) {
                return files.iterator().next();
            }
            
        } catch (Exception e) {
            // 忽略异常，尝试其他方法
        }
        
        return null;
    }
    
    /**
     * 通过类名查找文件
     */
    @Nullable
    private VirtualFile findByClassName(@NotNull String className) {
        try {
            // 获取简单类名
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            
            // 搜索类
            PsiClass[] classes = PsiShortNamesCache.getInstance(project)
                .getClassesByName(simpleClassName, GlobalSearchScope.projectScope(project));
            
            for (PsiClass psiClass : classes) {
                if (className.equals(psiClass.getQualifiedName())) {
                    return psiClass.getContainingFile().getVirtualFile();
                }
            }
            
            // 如果没有完全匹配，返回第一个匹配的类
            if (classes.length > 0) {
                return classes[0].getContainingFile().getVirtualFile();
            }
            
        } catch (Exception e) {
            // 忽略异常，尝试其他方法
        }
        
        return null;
    }
    
    /**
     * 通过本地文件系统查找文件
     */
    @Nullable
    private VirtualFile findByLocalFileSystem(@NotNull String fileName) {
        try {
            // 尝试作为绝对路径
            File file = new File(fileName);
            if (file.exists() && file.isFile()) {
                return LocalFileSystem.getInstance().findFileByIoFile(file);
            }
            
            // 尝试相对于项目根目录
            if (project.getBasePath() != null) {
                file = new File(project.getBasePath(), fileName);
                if (file.exists() && file.isFile()) {
                    return LocalFileSystem.getInstance().findFileByIoFile(file);
                }
            }
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return null;
    }
}
