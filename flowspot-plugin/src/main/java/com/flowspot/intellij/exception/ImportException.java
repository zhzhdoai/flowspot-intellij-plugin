/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.exception;

/**
 * Thrown when import operation fails
 */
public class ImportException extends GlobalSinkRulesException {
    public ImportException(String message) {
        super(message);
    }
    
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
