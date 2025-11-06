/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.actions;

import com.flowspot.intellij.gui.GlobalSinkRulesDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action to open the Global Sink Rules configuration dialog.
 * Placed under "Analyze Project Files" menu as per user requirement.
 */
public class ConfigureGlobalSinkRulesAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        GlobalSinkRulesDialog dialog = new GlobalSinkRulesDialog(project);
        dialog.show();
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Action is always enabled
        e.getPresentation().setEnabled(true);
    }
}
