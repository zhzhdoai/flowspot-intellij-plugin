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
package com.flowspot.intellij.validation;

import com.flowspot.intellij.model.MethodPattern;
import com.flowspot.intellij.model.SinkPattern;
import com.flowspot.intellij.model.SinkRule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Validator for Sink rules.
 * Provides validation for rule names, patterns, parameters, and complete rules.
 */
public class RuleValidator {
    
    // Rule name must be uppercase with underscores only
    private static final Pattern RULE_NAME_PATTERN = Pattern.compile("^[A-Z_][A-Z0-9_]*$");
    
    // Valid type names
    private static final Set<String> VALID_TYPE_NAMES = Set.of("CALL", "FIELD", "RETURN");
    
    // Priority range
    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 10;
    
    /**
     * Validate a complete sink rule
     */
    @NotNull
    public ValidationResult validate(@NotNull SinkRule rule) {
        List<String> errors = new ArrayList<>();
        
        // Validate name
        ValidationResult nameResult = validateName(rule.getName());
        if (nameResult.hasErrors()) {
            errors.addAll(nameResult.getErrors());
        }
        
        // Validate typeName
        ValidationResult typeResult = validateTypeName(rule.getTypeName());
        if (typeResult.hasErrors()) {
            errors.addAll(typeResult.getErrors());
        }
        
        // Validate priority
        ValidationResult priorityResult = validatePriority(rule.getPriority());
        if (priorityResult.hasErrors()) {
            errors.addAll(priorityResult.getErrors());
        }
        
        // Validate category
        if (rule.getCategory() == null || rule.getCategory().trim().isEmpty()) {
            errors.add("Category cannot be empty");
        }
        
        // Validate description
        if (rule.getDescription() == null || rule.getDescription().trim().isEmpty()) {
            errors.add("Description cannot be empty");
        }
        
        // Validate sinks
        if (rule.getSinks() == null || rule.getSinks().isEmpty()) {
            errors.add("Rule must have at least one sink pattern");
        } else {
            for (int i = 0; i < rule.getSinks().size(); i++) {
                SinkPattern sink = rule.getSinks().get(i);
                ValidationResult sinkResult = validateSinkPattern(sink);
                if (sinkResult.hasErrors()) {
                    for (String error : sinkResult.getErrors()) {
                        errors.add("Sink pattern " + (i + 1) + ": " + error);
                    }
                }
            }
        }
        
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.errors(errors);
    }
    
    /**
     * Validate rule name format
     */
    @NotNull
    public ValidationResult validateName(@NotNull String name) {
        if (name == null || name.trim().isEmpty()) {
            return ValidationResult.error("Rule name cannot be empty");
        }
        
        if (!RULE_NAME_PATTERN.matcher(name).matches()) {
            return ValidationResult.error("Rule name must be uppercase with underscores only (e.g., CUSTOM_SQL_INJECTION)");
        }
        
        return ValidationResult.ok();
    }
    
    /**
     * Validate type name
     */
    @NotNull
    public ValidationResult validateTypeName(@NotNull String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return ValidationResult.error("Type name cannot be empty");
        }
        
        if (!VALID_TYPE_NAMES.contains(typeName)) {
            return ValidationResult.error("Type name must be one of: " + String.join(", ", VALID_TYPE_NAMES));
        }
        
        return ValidationResult.ok();
    }
    
    /**
     * Validate method pattern regex
     */
    @NotNull
    public ValidationResult validateMethodPattern(@NotNull String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return ValidationResult.error("Method pattern cannot be empty");
        }
        
        try {
            Pattern.compile(pattern);
            return ValidationResult.ok();
        } catch (PatternSyntaxException e) {
            return ValidationResult.error("Invalid regex pattern: " + e.getMessage());
        }
    }
    
    /**
     * Validate parameter indices
     */
    @NotNull
    public ValidationResult validateParameterIndices(@NotNull List<String> indices) {
        if (indices == null) {
            return ValidationResult.error("Parameter indices cannot be null");
        }
        
        // Empty list is valid (matches any parameter)
        if (indices.isEmpty()) {
            return ValidationResult.ok();
        }
        
        List<String> errors = new ArrayList<>();
        for (String index : indices) {
            if (index == null || index.trim().isEmpty()) {
                errors.add("Parameter index cannot be empty");
                continue;
            }
            
            try {
                int value = Integer.parseInt(index.trim());
                if (value < 1) {
                    errors.add("Parameter index must be >= 1 (got: " + index + ")");
                }
            } catch (NumberFormatException e) {
                errors.add("Parameter index must be a number (got: " + index + ")");
            }
        }
        
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.errors(errors);
    }
    
    /**
     * Validate priority value
     */
    @NotNull
    public ValidationResult validatePriority(int priority) {
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            return ValidationResult.error("Priority must be between " + MIN_PRIORITY + " and " + MAX_PRIORITY + " (got: " + priority + ")");
        }
        return ValidationResult.ok();
    }
    
    /**
     * Validate sink pattern
     */
    @NotNull
    private ValidationResult validateSinkPattern(@NotNull SinkPattern sinkPattern) {
        List<String> errors = new ArrayList<>();
        
        // Validate typeName
        ValidationResult typeResult = validateTypeName(sinkPattern.getTypeName());
        if (typeResult.hasErrors()) {
            errors.addAll(typeResult.getErrors());
        }
        
        // Validate patterns
        if (sinkPattern.getPatterns() == null || sinkPattern.getPatterns().isEmpty()) {
            errors.add("Sink pattern must have at least one method pattern");
        } else {
            for (int i = 0; i < sinkPattern.getPatterns().size(); i++) {
                MethodPattern methodPattern = sinkPattern.getPatterns().get(i);
                ValidationResult methodResult = validateMethodPatternObject(methodPattern);
                if (methodResult.hasErrors()) {
                    for (String error : methodResult.getErrors()) {
                        errors.add("Method pattern " + (i + 1) + ": " + error);
                    }
                }
            }
        }
        
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.errors(errors);
    }
    
    /**
     * Validate method pattern object
     */
    @NotNull
    private ValidationResult validateMethodPatternObject(@NotNull MethodPattern methodPattern) {
        List<String> errors = new ArrayList<>();
        
        // Validate method regex
        ValidationResult methodResult = validateMethodPattern(methodPattern.getMethod());
        if (methodResult.hasErrors()) {
            errors.addAll(methodResult.getErrors());
        }
        
        // Validate tainted params
        ValidationResult paramsResult = validateParameterIndices(methodPattern.getTaintedParams());
        if (paramsResult.hasErrors()) {
            errors.addAll(paramsResult.getErrors());
        }
        
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.errors(errors);
    }
    
    /**
     * Validate uniqueness of rule names in a list
     */
    @NotNull
    public ValidationResult validateUniqueNames(@NotNull List<SinkRule> rules) {
        Set<String> names = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        
        for (SinkRule rule : rules) {
            if (!names.add(rule.getName())) {
                duplicates.add(rule.getName());
            }
        }
        
        if (duplicates.isEmpty()) {
            return ValidationResult.ok();
        }
        
        return ValidationResult.error("Duplicate rule names found: " + String.join(", ", duplicates));
    }
}
