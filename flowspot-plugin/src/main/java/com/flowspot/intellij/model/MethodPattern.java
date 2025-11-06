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
package com.flowspot.intellij.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a specific method signature pattern to detect.
 * Matches the existing sinks.json schema for compatibility.
 */
public class MethodPattern {
    
    /**
     * Regex pattern for method signature
     * Example: ".*com\\.example\\.DatabaseUtil\\.executeQuery.*"
     */
    @NotNull
    private String method;
    
    /**
     * Parameter indices that can be tainted (1-based)
     * Can be empty array to match any parameter
     */
    @NotNull
    private List<String> taintedParams;
    
    /**
     * Default constructor for JSON deserialization
     */
    public MethodPattern() {
        this.method = "";
        this.taintedParams = new ArrayList<>();
    }
    
    /**
     * Full constructor
     */
    public MethodPattern(@NotNull String method, @NotNull List<String> taintedParams) {
        this.method = method;
        this.taintedParams = new ArrayList<>(taintedParams);
    }
    
    // Getters and Setters
    
    @NotNull
    public String getMethod() {
        return method;
    }
    
    public void setMethod(@NotNull String method) {
        this.method = method;
    }
    
    @NotNull
    public List<String> getTaintedParams() {
        return new ArrayList<>(taintedParams);
    }
    
    public void setTaintedParams(@NotNull List<String> taintedParams) {
        this.taintedParams = new ArrayList<>(taintedParams);
    }
    
    /**
     * Add a tainted parameter index
     */
    public void addTaintedParam(@NotNull String paramIndex) {
        if (!this.taintedParams.contains(paramIndex)) {
            this.taintedParams.add(paramIndex);
        }
    }
    
    /**
     * Remove a tainted parameter index
     */
    public boolean removeTaintedParam(@NotNull String paramIndex) {
        return this.taintedParams.remove(paramIndex);
    }
    
    /**
     * Create a copy of this method pattern
     */
    public MethodPattern copy() {
        return new MethodPattern(method, new ArrayList<>(taintedParams));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodPattern)) return false;
        MethodPattern that = (MethodPattern) o;
        return method.equals(that.method) && taintedParams.equals(that.taintedParams);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(method, taintedParams);
    }
    
    @Override
    public String toString() {
        return "MethodPattern{" +
                "method='" + method + '\'' +
                ", taintedParams=" + taintedParams +
                '}';
    }
}
