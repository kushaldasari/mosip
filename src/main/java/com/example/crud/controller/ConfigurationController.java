package com.example.crud.controller;

import com.example.crud.config.RoutingConfigurationReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigurationController {

    @Autowired
    private RoutingConfigurationReader routingConfigReader;

    /**
     * Get current routing configuration
     */
    @GetMapping("/routing")
    public Map<String, Object> getRoutingConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("defaultRules", routingConfigReader.getDefaultRules());
        config.put("idBasedRules", routingConfigReader.getIdBasedRules());
        config.put("verticle1Config", routingConfigReader.getVerticleConfig("verticle1"));
        config.put("verticle2Config", routingConfigReader.getVerticleConfig("verticle2"));
        return config;
    }

    /**
     * Test routing for a specific operation and user ID
     */
    @GetMapping("/routing/test")
    public Map<String, Object> testRouting(
            @RequestParam String operation,
            @RequestParam(required = false) Long userId) {
        
        String destination = routingConfigReader.getDestinationForOperation(operation, userId);
        boolean supported = routingConfigReader.isOperationSupportedByVerticle(destination, operation);
        
        Map<String, Object> result = new HashMap<>();
        result.put("operation", operation);
        result.put("userId", userId);
        result.put("destination", destination);
        result.put("supported", supported);
        result.put("verticleConfig", routingConfigReader.getVerticleConfig(destination));
        
        return result;
    }

    /**
     * Get configuration for a specific verticle
     */
    @GetMapping("/verticle/{verticleName}")
    public RoutingConfigurationReader.VerticleConfig getVerticleConfiguration(
            @PathVariable String verticleName) {
        return routingConfigReader.getVerticleConfig(verticleName);
    }

    /**
     * Check if an operation is supported by a verticle
     */
    @GetMapping("/verticle/{verticleName}/supports/{operation}")
    public Map<String, Object> checkOperationSupport(
            @PathVariable String verticleName,
            @PathVariable String operation) {
        
        boolean supported = routingConfigReader.isOperationSupportedByVerticle(verticleName, operation);
        
        Map<String, Object> result = new HashMap<>();
        result.put("verticle", verticleName);
        result.put("operation", operation);
        result.put("supported", supported);
        
        return result;
    }

    /**
     * Get routing statistics and health check
     */
    @GetMapping("/health")
    public Map<String, Object> getConfigurationHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test basic configuration loading
            Map<String, String> defaultRules = routingConfigReader.getDefaultRules();
            Map<String, String> idBasedRules = routingConfigReader.getIdBasedRules();
            
            health.put("status", "healthy");
            health.put("defaultRulesCount", defaultRules.size());
            health.put("idBasedRulesCount", idBasedRules.size());
            health.put("verticle1Loaded", routingConfigReader.getVerticleConfig("verticle1") != null);
            health.put("verticle2Loaded", routingConfigReader.getVerticleConfig("verticle2") != null);
            
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}
