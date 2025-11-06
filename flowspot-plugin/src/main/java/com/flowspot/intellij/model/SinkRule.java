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
 * Represents a single Sink rule definition.
 * Matches the existing sinks.json schema for compatibility.
 */
public class SinkRule {
    
    /**
     * Unique rule identifier (e.g., "CUSTOM_SQL_INJECTION")
     * Pattern: ^[A-Z_][A-Z0-9_]*$
     */
    @NotNull
    private String name;
    
    /**
     * Rule type (e.g., "CALL", "FIELD", "RETURN")
     */
    @NotNull
    private String typeName;
    
    /**
     * Priority level (1-10, higher = more critical)
     */
    private int priority;
    
    /**
     * Category label (e.g., "代码执行", "文件操作")
     */
    @NotNull
    private String category;
    
    /**
     * Human-readable description
     */
    @NotNull
    private String description;
    
    /**
     * List of sink patterns
     */
    @NotNull
    private List<SinkPattern> sinks;
    
    /**
     * Default constructor for JSON deserialization
     */
    public SinkRule() {
        this.name = "";
        this.typeName = "CALL";
        this.priority = 5;
        this.category = "";
        this.description = "";
        this.sinks = new ArrayList<>();
    }
    
    /**
     * Full constructor
     */
    public SinkRule(@NotNull String name, @NotNull String typeName, int priority,
                    @NotNull String category, @NotNull String description,
                    @NotNull List<SinkPattern> sinks) {
        this.name = name;
        this.typeName = typeName;
        this.priority = priority;
        this.category = category;
        this.description = description;
        this.sinks = new ArrayList<>(sinks);
    }
    
    // Getters and Setters
    
    @NotNull
    public String getName() {
        return name;
    }
    
    public void setName(@NotNull String name) {
        this.name = name;
    }
    
    @NotNull
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(@NotNull String typeName) {
        this.typeName = typeName;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    @NotNull
    public String getCategory() {
        return category;
    }
    
    public void setCategory(@NotNull String category) {
        this.category = category;
    }
    
    @NotNull
    public String getDescription() {
        return description;
    }
    
    public void setDescription(@NotNull String description) {
        this.description = description;
    }
    
    @NotNull
    public List<SinkPattern> getSinks() {
        return new ArrayList<>(sinks);
    }
    
    public void setSinks(@NotNull List<SinkPattern> sinks) {
        this.sinks = new ArrayList<>(sinks);
    }
    
    // Utility methods
    
    /**
     * Get total number of method patterns across all sinks
     */
    public int getPatternCount() {
        return sinks.stream()
                .mapToInt(s -> s.getPatterns().size())
                .sum();
    }
    
    /**
     * Create a copy of this rule
     */
    public SinkRule copy() {
        List<SinkPattern> copiedSinks = new ArrayList<>();
        for (SinkPattern sink : sinks) {
            copiedSinks.add(sink.copy());
        }
        return new SinkRule(name, typeName, priority, category, description, copiedSinks);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkRule)) return false;
        SinkRule sinkRule = (SinkRule) o;
        return name.equals(sinkRule.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "SinkRule{" +
                "name='" + name + '\'' +
                ", typeName='" + typeName + '\'' +
                ", priority=" + priority +
                ", category='" + category + '\'' +
                ", patternCount=" + getPatternCount() +
                '}';
    }
}
