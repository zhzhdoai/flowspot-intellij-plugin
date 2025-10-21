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
package com.flowspot.intellij.core;

import com.flowspot.intellij.model.*;
import omni.flowspot.core.FlowSpotBugInstance;
import omni.flowspot.annotations.FlowSpotSourceLineAnnotation;
import omni.flowspot.annotations.FlowSpotEnhancedSourceLineAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FlowSpot 结果处理器
 * 完全独立于 SpotBugs，直接处理 FlowSpot 的分析结果
 */
public class FlowSpotResultProcessor {
    
    /**
     * 处理 FlowSpot 分析结果，转换为 UI 数据模型
     */
    @NotNull
    public static FlowSpotVulnerabilityCollection processResults(
            @NotNull List<FlowSpotBugInstance> bugInstances,
            @NotNull String projectName) {
        return processResults(bugInstances, projectName, null);
    }
    
    @NotNull
    public static FlowSpotVulnerabilityCollection processResults(
            @NotNull List<FlowSpotBugInstance> bugInstances,
            @NotNull String projectName,
            @Nullable String analysisBasePath) {
        
        FlowSpotVulnerabilityCollection collection = new FlowSpotVulnerabilityCollection(projectName, analysisBasePath);
        
        for (FlowSpotBugInstance bugInstance : bugInstances) {
            try {
                FlowSpotVulnerability vulnerability = convertBugInstance(bugInstance);
                if (vulnerability != null) {
                    collection.addVulnerability(vulnerability);
                }
            } catch (Exception e) {
                System.err.println("Failed to convert bug instance: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return collection;
    }
    
    /**
     * 转换 FlowSpotBugInstance 为 FlowSpotVulnerability
     */
    @Nullable
    private static FlowSpotVulnerability convertBugInstance(@NotNull FlowSpotBugInstance bugInstance) {
        // 生成唯一ID
        String id = generateVulnerabilityId(bugInstance);
        
        // 基本信息
        String type = bugInstance.getType();
        String category = bugInstance.getCategory();
        String title = generateTitle(bugInstance);
        String description = extractDescription(bugInstance);
        int priority = bugInstance.getPriority();
        String severity = mapPriorityToSeverity(priority);
        
        // 位置信息
        FlowSpotLocation primaryLocation = extractPrimaryLocation(bugInstance);
        List<FlowSpotLocation> dataFlowPath = extractDataFlowPath(bugInstance);
        
        // 注解信息
        List<FlowSpotAnnotation> annotations = extractAnnotations(bugInstance);
        
        // HTML详情
        String htmlDetails = generateHtmlDetails(bugInstance);
        
        return new FlowSpotVulnerability(
            id, type, category, title, description, priority, severity,
            primaryLocation, dataFlowPath, annotations, htmlDetails, bugInstance
        );
    }
    
    /**
     * 生成漏洞唯一ID
     */
    @NotNull
    private static String generateVulnerabilityId(@NotNull FlowSpotBugInstance bugInstance) {
        StringBuilder sb = new StringBuilder();
        sb.append(bugInstance.getType());
        
        // 尝试从主要注解获取位置信息
        FlowSpotSourceLineAnnotation primaryAnnotation = bugInstance.getPrimarySourceLineAnnotation();
        if (primaryAnnotation != null) {
            sb.append("_").append(primaryAnnotation.getClassName());
            sb.append("_").append(primaryAnnotation.getStartLine());
        }
        
        // 如果无法生成稳定ID，使用随机UUID
        if (sb.length() <= bugInstance.getType().length()) {
            return UUID.randomUUID().toString();
        }
        
        return sb.toString().replaceAll("[^a-zA-Z0-9_]", "_");
    }
    
    /**
     * 提取分类信息
     */
    @NotNull
    private static String extractCategory(@NotNull FlowSpotBugInstance bugInstance) {
        // 默认为安全类别，因为 FlowSpot 主要检测安全漏洞
        return bugInstance.getCategory();
    }
    
    /**
     * 生成标题 - 动态处理漏洞类型
     */
    @NotNull
    private static String generateTitle(@NotNull FlowSpotBugInstance bugInstance) {
        String type = bugInstance.getType();
        
        // 动态转换类型为可读标题
        return formatVulnerabilityType(type) + " Vulnerability";
    }
    
    /**
     * 格式化漏洞类型为可读形式
     */
    @NotNull
    private static String formatVulnerabilityType(@NotNull String type) {
        if (type == null || type.trim().isEmpty()) {
            return "Unknown";
        }
        
        // 移除常见的前缀和后缀
        String cleanType = type.toLowerCase()
            .replaceAll("^(flowspot_|omni_)", "")  // 移除前缀
            .replaceAll("(_tagger|_query|_sink|_source)$", ""); // 移除后缀
        
        // 处理常见的缩写和特殊情况
        cleanType = expandCommonAbbreviations(cleanType);
        
        // 将下划线替换为空格，并进行标题格式化
        String[] words = cleanType.split("_");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            
            String word = words[i].trim();
            if (!word.isEmpty()) {
                // 首字母大写
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 展开常见的缩写
     */
    @NotNull
    private static String expandCommonAbbreviations(@NotNull String type) {
        return type
            .replace("xss", "cross_site_scripting")
            .replace("csrf", "cross_site_request_forgery")
            .replace("ssrf", "server_side_request_forgery")
            .replace("xxe", "xml_external_entity")
            .replace("lfi", "local_file_inclusion")
            .replace("rfi", "remote_file_inclusion")
            .replace("rce", "remote_code_execution")
            .replace("sqli", "sql_injection")
            .replace("nosqli", "nosql_injection")
            .replace("ldapi", "ldap_injection")
            .replace("cmdi", "command_injection")
            .replace("idor", "insecure_direct_object_reference")
            .replace("ssti", "server_side_template_injection")
            .replace("csti", "client_side_template_injection");
    }
    
    /**
     * 提取描述信息 - 动态生成描述
     */
    @NotNull
    private static String extractDescription(@NotNull FlowSpotBugInstance bugInstance) {
        String message = bugInstance.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        
        // 动态生成默认描述
        String type = bugInstance.getType();
        String formattedType = formatVulnerabilityType(type).toLowerCase();
        
        return String.format(
            "FlowSpot detected a potential %s vulnerability in the code. " +
            "This indicates that user input may flow to a sensitive sink without proper validation or sanitization. " +
            "Please review the data flow path and implement appropriate security measures.",
            formattedType
        );
    }
    
    /**
     * 映射优先级到严重程度
     */
    @NotNull
    private static String mapPriorityToSeverity(int priority) {
        switch (priority) {
            case 1: return "High";
            case 2: return "Medium";
            case 3: return "Low";
            default: return "Unknown";
        }
    }
    
    /**
     * 提取主要位置信息
     */
    @Nullable
    private static FlowSpotLocation extractPrimaryLocation(@NotNull FlowSpotBugInstance bugInstance) {
        FlowSpotSourceLineAnnotation sourceAnnotation = bugInstance.getPrimarySourceLineAnnotation();
        if (sourceAnnotation != null) {
            return convertSourceLineAnnotationToLocation(sourceAnnotation);
        }
        return null;
    }
    
    /**
     * 提取数据流路径
     */
    @NotNull
    private static List<FlowSpotLocation> extractDataFlowPath(@NotNull FlowSpotBugInstance bugInstance) {
        List<FlowSpotLocation> dataFlowPath = new ArrayList<>();
        
        // 遍历所有注解，提取位置信息
        for (FlowSpotSourceLineAnnotation annotation : bugInstance.getAnnotations()) {
            FlowSpotLocation location = convertSourceLineAnnotationToLocation(annotation);
            if (location != null) {
                dataFlowPath.add(location);
            }
        }
        
        return dataFlowPath;
    }
    
    /**
     * 提取注解信息
     */
    @NotNull
    private static List<FlowSpotAnnotation> extractAnnotations(@NotNull FlowSpotBugInstance bugInstance) {
        List<FlowSpotAnnotation> annotations = new ArrayList<>();
        
        for (FlowSpotSourceLineAnnotation sourceAnnotation : bugInstance.getAnnotations()) {
            FlowSpotAnnotation.AnnotationType type = FlowSpotAnnotation.AnnotationType.SOURCE_LINE;
            String description = sourceAnnotation.getDescription();
            FlowSpotLocation location = convertSourceLineAnnotationToLocation(sourceAnnotation);
            String pattern = null;
            String value = null;
            
            // 如果是增强注解，提取额外信息
            if (sourceAnnotation instanceof FlowSpotEnhancedSourceLineAnnotation) {
                FlowSpotEnhancedSourceLineAnnotation enhanced = 
                    (FlowSpotEnhancedSourceLineAnnotation) sourceAnnotation;
                type = FlowSpotAnnotation.AnnotationType.ENHANCED_SOURCE_LINE;
                pattern = enhanced.getPattern();
                if (enhanced.getMethodName() != null) {
                    value = enhanced.getMethodName();
                }
            }
            
            FlowSpotAnnotation annotation = new FlowSpotAnnotation(
                type, description != null ? description : "Source line annotation", 
                location, pattern, value
            );
            annotations.add(annotation);
        }
        
        return annotations;
    }
    
    /**
     * 转换 SourceLineAnnotation 为 FlowSpotLocation
     */
    @Nullable
    private static FlowSpotLocation convertSourceLineAnnotationToLocation(
            @NotNull FlowSpotSourceLineAnnotation annotation) {
        
        String className = annotation.getClassName();
        String fileName = annotation.getSourceFile();
        String methodName = null;
        
        // 尝试从增强注解获取方法名
        if (annotation instanceof FlowSpotEnhancedSourceLineAnnotation) {
            FlowSpotEnhancedSourceLineAnnotation enhanced = 
                (FlowSpotEnhancedSourceLineAnnotation) annotation;
            if (enhanced.getMethodName() != null) {
                methodName = enhanced.getMethodName();
            }
        }
        
        int startLine = annotation.getStartLine();
        int endLine = annotation.getEndLine();
        int startColumn = annotation.getStartBytecode();
        int endColumn = annotation.getEndBytecode();
        
        String identifierName = null;
        if (annotation.getIdentifierName() != null) {
            identifierName = annotation.getIdentifierName();
        }
        
        return new FlowSpotLocation(
            className != null ? className : "Unknown",
            fileName != null ? fileName : "Unknown",
            methodName,
            startLine,
            endLine,
            startColumn,
            endColumn,
            "SourceLine",
            identifierName,
            annotation.getDescription()
        );
    }
    
    /**
     * 生成HTML详情 - 动态格式化
     */
    @NotNull
    private static String generateHtmlDetails(@NotNull FlowSpotBugInstance bugInstance) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: sans-serif; margin: 10px;'>");
        
        // 使用格式化的标题
        String formattedTitle = generateTitle(bugInstance);
        sb.append("<h2>").append(escapeHtml(formattedTitle)).append("</h2>");
        
        String message = bugInstance.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            sb.append("<p>").append(escapeHtml(message)).append("</p>");
        } else {
            // 使用动态生成的描述
            String description = extractDescription(bugInstance);
            sb.append("<p>").append(escapeHtml(description)).append("</p>");
        }
        
        sb.append("<h3>Vulnerability Details</h3>");
        sb.append("<ul>");
        sb.append("<li><strong>Original Type:</strong> ").append(escapeHtml(bugInstance.getType())).append("</li>");
        sb.append("<li><strong>Formatted Type:</strong> ").append(escapeHtml(formatVulnerabilityType(bugInstance.getType()))).append("</li>");
        sb.append("<li><strong>Priority:</strong> ").append(bugInstance.getPriority()).append("</li>");
        sb.append("<li><strong>Severity:</strong> ").append(escapeHtml(mapPriorityToSeverity(bugInstance.getPriority()))).append("</li>");
        sb.append("<li><strong>Category:</strong> ").append(escapeHtml(extractCategory(bugInstance))).append("</li>");
        sb.append("</ul>");
        
        // 添加数据流信息
        sb.append("<h3>Data Flow Analysis</h3>");
        sb.append("<p>This vulnerability was detected through FlowSpot's data flow analysis, ");
        sb.append("which traces the flow of potentially tainted data from sources to sinks. ");
        sb.append("Review the data flow path in the Details panel to understand how user input ");
        sb.append("reaches sensitive operations without proper validation.</p>");
        
        sb.append("</body></html>");
        
        return sb.toString();
    }
    
    /**
     * HTML转义
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
