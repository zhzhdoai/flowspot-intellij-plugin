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

/**
 * Persistent state container for global Sink rules.
 * Used by IntelliJ Platform's PersistentStateComponent.
 */
public class GlobalSinkRulesState {
    
    /**
     * Schema version for compatibility
     */
    public String version = "1.0";
    
    /**
     * JSON string containing array of SinkRule objects
     * Stored as string to maintain compatibility with existing sinks.json format
     */
    public String rulesJson = "[]";
    
    /**
     * Timestamp of last modification (milliseconds since epoch)
     */
    public long lastModified = System.currentTimeMillis();
    
    /**
     * Default constructor for XML serialization
     */
    public GlobalSinkRulesState() {
    }
    
    /**
     * Copy constructor
     */
    public GlobalSinkRulesState(String version, String rulesJson, long lastModified) {
        this.version = version;
        this.rulesJson = rulesJson;
        this.lastModified = lastModified;
    }
    
    /**
     * Update last modified timestamp to current time
     */
    public void touch() {
        this.lastModified = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "GlobalSinkRulesState{" +
                "version='" + version + '\'' +
                ", rulesJsonLength=" + (rulesJson != null ? rulesJson.length() : 0) +
                ", lastModified=" + lastModified +
                '}';
    }
}
