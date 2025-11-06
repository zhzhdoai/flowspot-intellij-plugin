/*
 * Copyright 2024 FlowSpot plugin contributors
 */
package com.flowspot.intellij.service.impl;

import com.flowspot.intellij.exception.NotFoundException;
import com.flowspot.intellij.exception.ValidationException;
import com.flowspot.intellij.model.GlobalSinkRulesState;
import com.flowspot.intellij.model.SinkRule;
import com.flowspot.intellij.service.GlobalSinkRulesService;
import com.flowspot.intellij.validation.RuleValidator;
import com.flowspot.intellij.validation.ValidationResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.RoamingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of GlobalSinkRulesService with persistent state.
 * 存储为 JSON 格式在 options/flowspot-global-sinks.json
 */
@State(
    name = "FlowSpotGlobalSinkRules",
    storages = @Storage(value = "flowspot-global-sinks.json", roamingType = RoamingType.DISABLED)
)
public class GlobalSinkRulesServiceImpl implements GlobalSinkRulesService {
    
    private GlobalSinkRulesState state = new GlobalSinkRulesState();
    private final RuleValidator validator = new RuleValidator();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Type ruleListType = new TypeToken<List<SinkRule>>(){}.getType();
    
    @Override
    public GlobalSinkRulesState getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull GlobalSinkRulesState state) {
        this.state = state;
    }
    
    @Override
    @NotNull
    public List<SinkRule> getRules() {
        try {
            // 如果是首次使用（空状态），加载默认规则
            if (state.rulesJson == null || state.rulesJson.trim().isEmpty() || state.rulesJson.equals("[]")) {
                loadDefaultRulesIfNeeded();
            }
            
            if (state.rulesJson == null || state.rulesJson.trim().isEmpty() || state.rulesJson.equals("[]")) {
                return new ArrayList<>();
            }
            
            List<SinkRule> rules = gson.fromJson(state.rulesJson, ruleListType);
            return rules != null ? new ArrayList<>(rules) : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            // Log error and return empty list
            return new ArrayList<>();
        }
    }
    
    /**
     * 首次使用时加载默认规则
     * 从插件资源目录 flowspot-plugin/src/main/resources/config/sinks.json 复制到全局位置
     */
    private void loadDefaultRulesIfNeeded() {
        try {
            System.out.println("[GlobalSinkRulesService] Loading default sinks.json from plugin resources");
            
            // 从插件资源加载默认 sinks.json
            java.io.InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("config/sinks.json");
            
            if (inputStream == null) {
                System.err.println("[GlobalSinkRulesService] Default sinks.json not found in plugin resources");
                return;
            }
            
            // 读取资源文件内容
            String defaultJson = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            inputStream.close();
            
            // 保存到全局状态
            state.rulesJson = defaultJson;
            state.touch();
            
            System.out.println("[GlobalSinkRulesService] Loaded default rules from plugin resources");
            System.out.println("[GlobalSinkRulesService] JSON length: " + defaultJson.length());
            System.out.println("[GlobalSinkRulesService] Rules count: " + getRules().size());
            System.out.println("[GlobalSinkRulesService] State saved to global storage");
            
        } catch (Exception e) {
            System.err.println("[GlobalSinkRulesService] Error loading default rules: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void saveRules(@NotNull List<SinkRule> rules) throws ValidationException {
        // 直接保存，不进行验证
        state.rulesJson = gson.toJson(rules);
        state.touch();
    }
    
    @Override
    public void addRule(@NotNull SinkRule rule) throws ValidationException {
        // 直接添加，不验证
        List<SinkRule> currentRules = getRules();
        currentRules.add(rule);
        saveRules(currentRules);
    }
    
    @Override
    public void updateRule(@NotNull String oldName, @NotNull SinkRule newRule) 
            throws ValidationException, NotFoundException {
        List<SinkRule> currentRules = getRules();
        
        // Find rule with old name
        int index = -1;
        for (int i = 0; i < currentRules.size(); i++) {
            if (currentRules.get(i).getName().equals(oldName)) {
                index = i;
                break;
            }
        }
        
        if (index == -1) {
            throw new NotFoundException(oldName);
        }
        
        // Update rule (不验证)
        currentRules.set(index, newRule);
        saveRules(currentRules);
    }
    
    @Override
    public boolean deleteRule(@NotNull String name) {
        List<SinkRule> currentRules = getRules();
        boolean removed = currentRules.removeIf(r -> r.getName().equals(name));
        
        if (removed) {
            try {
                saveRules(currentRules);
            } catch (ValidationException e) {
                // Should not happen since we're just removing
                return false;
            }
        }
        
        return removed;
    }
    
    @Override
    @Nullable
    public SinkRule getRule(@NotNull String name) {
        return getRules().stream()
                .filter(r -> r.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public boolean ruleExists(@NotNull String name) {
        return getRules().stream().anyMatch(r -> r.getName().equals(name));
    }
    
    @Override
    @NotNull
    public ValidationResult validateRule(@NotNull SinkRule rule) {
        return validator.validate(rule);
    }
    
    @Override
    @NotNull
    public String getRulesAsJson() {
        if (state.rulesJson == null || state.rulesJson.trim().isEmpty()) {
            return "[]";
        }
        return state.rulesJson;
    }
    
    @Override
    public int importRulesFromJson(@NotNull String json, boolean replaceExisting) throws ValidationException {
        try {
            List<SinkRule> importedRules = gson.fromJson(json, ruleListType);
            if (importedRules == null) {
                throw new ValidationException(List.of("Invalid JSON: parsed to null"));
            }
            
            // 不验证规则，直接导入
            if (replaceExisting) {
                saveRules(importedRules);
                return importedRules.size();
            } else {
                // Merge: keep existing, add new (skip duplicates)
                List<SinkRule> currentRules = getRules();
                List<String> existingNames = currentRules.stream()
                        .map(SinkRule::getName)
                        .collect(Collectors.toList());
                
                int addedCount = 0;
                for (SinkRule importedRule : importedRules) {
                    if (!existingNames.contains(importedRule.getName())) {
                        currentRules.add(importedRule);
                        addedCount++;
                    }
                }
                
                saveRules(currentRules);
                return addedCount;
            }
        } catch (JsonSyntaxException e) {
            throw new ValidationException(List.of("Invalid JSON syntax: " + e.getMessage()));
        }
    }
}
