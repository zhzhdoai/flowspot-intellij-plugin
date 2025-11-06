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
 * Represents a pattern group within a Sink rule.
 * Matches the existing sinks.json schema for compatibility.
 */
public class SinkPattern {
    
    /**
     * Pattern type (e.g., "CALL", "FIELD")
     */
    @NotNull
    private String typeName;
    
    /**
     * List of method patterns
     */
    @NotNull
    private List<MethodPattern> patterns;
    
    /**
     * Default constructor for JSON deserialization
     */
    public SinkPattern() {
        this.typeName = "CALL";
        this.patterns = new ArrayList<>();
    }
    
    /**
     * Full constructor
     */
    public SinkPattern(@NotNull String typeName, @NotNull List<MethodPattern> patterns) {
        this.typeName = typeName;
        this.patterns = new ArrayList<>(patterns);
    }
    
    // Getters and Setters
    
    @NotNull
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(@NotNull String typeName) {
        this.typeName = typeName;
    }
    
    @NotNull
    public List<MethodPattern> getPatterns() {
        return new ArrayList<>(patterns);
    }
    
    public void setPatterns(@NotNull List<MethodPattern> patterns) {
        this.patterns = new ArrayList<>(patterns);
    }
    
    /**
     * Add a method pattern to this sink pattern
     */
    public void addPattern(@NotNull MethodPattern pattern) {
        this.patterns.add(pattern);
    }
    
    /**
     * Remove a method pattern from this sink pattern
     */
    public boolean removePattern(@NotNull MethodPattern pattern) {
        return this.patterns.remove(pattern);
    }
    
    /**
     * Create a copy of this sink pattern
     */
    public SinkPattern copy() {
        List<MethodPattern> copiedPatterns = new ArrayList<>();
        for (MethodPattern pattern : patterns) {
            copiedPatterns.add(pattern.copy());
        }
        return new SinkPattern(typeName, copiedPatterns);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkPattern)) return false;
        SinkPattern that = (SinkPattern) o;
        return typeName.equals(that.typeName) && patterns.equals(that.patterns);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(typeName, patterns);
    }
    
    @Override
    public String toString() {
        return "SinkPattern{" +
                "typeName='" + typeName + '\'' +
                ", patternCount=" + patterns.size() +
                '}';
    }
}
