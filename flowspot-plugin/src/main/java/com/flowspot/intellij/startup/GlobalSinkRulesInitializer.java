/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.startup;

import com.flowspot.intellij.service.GlobalSinkRulesService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * 插件启动时初始化全局 Sink 规则
 * 如果全局规则为空，从当前项目的 sinks.json 加载
 */
public class GlobalSinkRulesInitializer implements StartupActivity {
    
    @Override
    public void runActivity(@NotNull Project project) {
        // 在后台线程中初始化
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                System.out.println("[GlobalSinkRulesInitializer] Initializing global sink rules for project: " + project.getName());
                
                GlobalSinkRulesService service = GlobalSinkRulesService.getInstance();
                
                // 触发 getRules()，如果为空会自动加载默认规则
                int rulesCount = service.getRules().size();
                
                System.out.println("[GlobalSinkRulesInitializer] Global sink rules initialized: " + rulesCount + " rules");
                
            } catch (Exception e) {
                System.err.println("[GlobalSinkRulesInitializer] Failed to initialize global sink rules: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
