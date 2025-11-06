/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.exception;

/**
 * Thrown when a rule is not found
 */
public class NotFoundException extends GlobalSinkRulesException {
    public NotFoundException(String ruleName) {
        super("Rule not found: " + ruleName);
    }
}
