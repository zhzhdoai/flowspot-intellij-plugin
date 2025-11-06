/*
 * Copyright 2024 FlowSpot plugin contributors
 *
 * This file is part of IntelliJ FlowSpot plugin.
 */
package com.flowspot.intellij.service;

import com.flowspot.intellij.exception.NotFoundException;
import com.flowspot.intellij.exception.ValidationException;
import com.flowspot.intellij.model.GlobalSinkRulesState;
import com.flowspot.intellij.model.SinkRule;
import com.flowspot.intellij.validation.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Application-level service for managing global sink rules.
 * Implements PersistentStateComponent for automatic persistence.
 */
public interface GlobalSinkRulesService extends PersistentStateComponent<GlobalSinkRulesState> {
    
    /**
     * Get singleton instance of the service
     */
    @NotNull
    static GlobalSinkRulesService getInstance() {
        return ApplicationManager.getApplication().getService(GlobalSinkRulesService.class);
    }
    
    /**
     * Get all global sink rules
     * @return List of sink rules, never null (empty list if no rules)
     */
    @NotNull
    List<SinkRule> getRules();
    
    /**
     * Save global sink rules
     * @param rules List of rules to save
     * @throws ValidationException if rules are invalid
     */
    void saveRules(@NotNull List<SinkRule> rules) throws ValidationException;
    
    /**
     * Add a new sink rule
     * @param rule Rule to add
     * @throws ValidationException if rule is invalid or name conflicts
     */
    void addRule(@NotNull SinkRule rule) throws ValidationException;
    
    /**
     * Update an existing sink rule
     * @param oldName Original rule name
     * @param newRule Updated rule
     * @throws ValidationException if rule is invalid
     * @throws NotFoundException if oldName doesn't exist
     */
    void updateRule(@NotNull String oldName, @NotNull SinkRule newRule) 
        throws ValidationException, NotFoundException;
    
    /**
     * Delete a sink rule by name
     * @param name Rule name to delete
     * @return true if deleted, false if not found
     */
    boolean deleteRule(@NotNull String name);
    
    /**
     * Get a specific rule by name
     * @param name Rule name
     * @return Rule if found, null otherwise
     */
    @Nullable
    SinkRule getRule(@NotNull String name);
    
    /**
     * Check if a rule name exists
     * @param name Rule name to check
     * @return true if exists
     */
    boolean ruleExists(@NotNull String name);
    
    /**
     * Validate a rule without saving
     * @param rule Rule to validate
     * @return ValidationResult with errors if invalid
     */
    @NotNull
    ValidationResult validateRule(@NotNull SinkRule rule);
    
    /**
     * Get rules as JSON string (for analysis engine)
     * @return JSON string in sinks.json format
     */
    @NotNull
    String getRulesAsJson();
    
    /**
     * Import rules from JSON string
     * @param json JSON string containing rules
     * @param replaceExisting If true, replace all existing rules; if false, merge
     * @return Number of rules imported
     * @throws ValidationException if JSON is invalid
     */
    int importRulesFromJson(@NotNull String json, boolean replaceExisting) throws ValidationException;
}
