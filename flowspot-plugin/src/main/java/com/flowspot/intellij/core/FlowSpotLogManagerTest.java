// /*
//  * Copyright 2024 FlowSpot plugin contributors
//  *
//  * This file is part of IntelliJ FlowSpot plugin.
//  *
//  * IntelliJ FlowSpot plugin is free software: you can redistribute it 
//  * and/or modify it under the terms of the GNU General Public License
//  * as published by the Free Software Foundation, either version 3 of 
//  * the License, or (at your option) any later version.
//  *
//  * IntelliJ FlowSpot plugin is distributed in the hope that it will
//  * be useful, but WITHOUT ANY WARRANTY; without even the implied 
//  * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  * See the GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License
//  * along with IntelliJ FlowSpot plugin.
//  * If not, see <http://www.gnu.org/licenses/>.
//  */
// package com.flowspot.intellij.core;

// /**
//  * FlowSpot 日志管理器测试工具
//  * 用于验证日志系统的正确性
//  */
// public class FlowSpotLogManagerTest {
    
//     /**
//      * 测试日志系统的基本功能
//      */
//     public static void testLogging() {
//         System.out.println("=== FlowSpot 日志系统测试 ===");
        
//         // 测试全局日志
//         FlowSpotLogManager.info("测试信息日志");
//         FlowSpotLogManager.warn("测试警告日志");
//         FlowSpotLogManager.debug("测试调试日志");
//         FlowSpotLogManager.progress("测试进度日志");
//         FlowSpotLogManager.error("测试错误日志");
        
//         // 获取全局实例并测试
//         FlowSpotLogManager globalManager = FlowSpotLogManager.getGlobalInstance();
//         System.out.println("全局日志文件路径: " + globalManager.getLogFilePath());
//         System.out.println("固定日志文件名: " + FlowSpotLogManager.getFixedLogFileName());
        
//         // 测试异常日志
//         try {
//             throw new RuntimeException("测试异常");
//         } catch (Exception e) {
//             FlowSpotLogManager.error("捕获到测试异常", e);
//         }
        
//         System.out.println("=== 日志测试完成 ===");
//         System.out.println("请检查日志文件: " + globalManager.getLogFilePath());
//     }
    
//     /**
//      * 主方法，用于独立测试
//      */
//     public static void main(String[] args) {
//         testLogging();
//     }
// }
