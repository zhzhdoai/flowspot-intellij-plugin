/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.exception;

/**
 * Base exception for global sink rules operations
 */
public class GlobalSinkRulesException extends Exception {
    public GlobalSinkRulesException(String message) {
        super(message);
    }
    
    public GlobalSinkRulesException(String message, Throwable cause) {
        super(message, cause);
    }
}
