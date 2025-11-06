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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a validation operation.
 * Immutable class that holds validation status and error messages.
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    
    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
    }
    
    /**
     * Create a successful validation result
     */
    @NotNull
    public static ValidationResult ok() {
        return new ValidationResult(true, Collections.emptyList());
    }
    
    /**
     * Create a failed validation result with a single error message
     */
    @NotNull
    public static ValidationResult error(@NotNull String message) {
        return new ValidationResult(false, Collections.singletonList(message));
    }
    
    /**
     * Create a failed validation result with multiple error messages
     */
    @NotNull
    public static ValidationResult errors(@NotNull List<String> messages) {
        if (messages.isEmpty()) {
            return ok();
        }
        return new ValidationResult(false, messages);
    }
    
    /**
     * Check if validation passed
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Check if validation failed
     */
    public boolean hasErrors() {
        return !valid;
    }
    
    /**
     * Get all error messages
     */
    @NotNull
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * Get the first error message, or empty string if no errors
     */
    @NotNull
    public String getFirstError() {
        return errors.isEmpty() ? "" : errors.get(0);
    }
    
    /**
     * Get all errors as a single formatted string
     */
    @NotNull
    public String getErrorsAsString() {
        return String.join("\n", errors);
    }
    
    /**
     * Get number of errors
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * Combine this result with another result
     * Result is valid only if both are valid
     */
    @NotNull
    public ValidationResult and(@NotNull ValidationResult other) {
        if (this.valid && other.valid) {
            return ok();
        }
        
        List<String> combinedErrors = new ArrayList<>(this.errors);
        combinedErrors.addAll(other.errors);
        return errors(combinedErrors);
    }
    
    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        }
        return "ValidationResult{valid=false, errors=" + errors + "}";
    }
}
