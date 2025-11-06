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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FlowSpot 日志系统测试工具
 * 用于验证日志配置是否正确工作
 */
public class FlowSpotLogTest {
    
    public static void main(String[] args) {
        System.out.println("=== FlowSpot 日志系统测试 ===");
        
        // 测试日志目录
        String logDir = System.getProperty("user.home") + "/.flowspot";
        System.out.println("预期日志目录: " + logDir);
        
        Path logDirPath = Paths.get(logDir);
        if (!Files.exists(logDirPath)) {
            try {
                Files.createDirectories(logDirPath);
                System.out.println("✅ 创建日志目录成功: " + logDirPath.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("❌ 创建日志目录失败: " + e.getMessage());
                return;
            }
        } else {
            System.out.println("✅ 日志目录已存在: " + logDirPath.toAbsolutePath());
        }
        
        // 测试日志文件路径
        String logFilePath = logDir + "/flowspot-analysis.log";
        String errorLogFilePath = logDir + "/flowspot-error.log";
        
        System.out.println("预期日志文件: " + logFilePath);
        System.out.println("预期错误日志文件: " + errorLogFilePath);
        
        // 设置系统属性
        System.setProperty("flowspot.log.dir", logDir);
        System.out.println("设置系统属性 flowspot.log.dir = " + logDir);
        
        // 测试全局日志管理器
        try {
            FlowSpotLogManager globalManager = FlowSpotLogManager.getGlobalInstance();
            
            // 写入测试日志
            globalManager.logInfo("=== 日志系统测试开始 ===");
            globalManager.logInfo("测试信息日志记录");
            globalManager.logWarning("测试警告日志记录");
            globalManager.logError("测试错误日志记录");
            globalManager.logInfo("=== 日志系统测试结束 ===");
            
            System.out.println("✅ 日志写入测试完成");
            
            // 检查日志文件是否存在
            File logFile = new File(logFilePath);
            if (logFile.exists() && logFile.length() > 0) {
                System.out.println("✅ 日志文件创建成功，大小: " + logFile.length() + " 字节");
            } else {
                System.out.println("❌ 日志文件未创建或为空");
            }
            
            File errorLogFile = new File(errorLogFilePath);
            if (errorLogFile.exists() && errorLogFile.length() > 0) {
                System.out.println("✅ 错误日志文件创建成功，大小: " + errorLogFile.length() + " 字节");
            } else {
                System.out.println("⚠️  错误日志文件未创建或为空（正常，因为只有ERROR级别才会写入）");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 日志系统测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 测试完成 ===");
        System.out.println("请检查以下位置的日志文件:");
        System.out.println("  主日志: " + logFilePath);
        System.out.println("  错误日志: " + errorLogFilePath);
    }
}
