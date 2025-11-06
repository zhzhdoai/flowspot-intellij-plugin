/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.exception;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when rule validation fails
 */
public class ValidationException extends GlobalSinkRulesException {
    private final List<String> errors;
    
    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
        this.errors = new ArrayList<>(errors);
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
}
