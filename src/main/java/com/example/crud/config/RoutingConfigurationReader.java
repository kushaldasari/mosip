package com.example.crud.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class RoutingConfigurationReader {

    private Map<String, String> defaultRules = new HashMap<>();
    private Map<String, String> idBasedRules = new HashMap<>();
    private Map<String, VerticleConfig> verticleConfigs = new HashMap<>();

    public RoutingConfigurationReader() {
        loadRoutingConfiguration();
        loadVerticleConfigurations();
    }

    private void loadRoutingConfiguration() {
        try {
            ClassPathResource resource = new ClassPathResource("routing-rules.xml");
            InputStream inputStream = resource.getInputStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            // Load default rules
            NodeList defaultRuleNodes = document.getElementsByTagName("default-rules");
            if (defaultRuleNodes.getLength() > 0) {
                Element defaultRulesElement = (Element) defaultRuleNodes.item(0);
                NodeList ruleNodes = defaultRulesElement.getElementsByTagName("rule");
                
                for (int i = 0; i < ruleNodes.getLength(); i++) {
                    Element ruleElement = (Element) ruleNodes.item(i);
                    String operation = ruleElement.getAttribute("operation");
                    String destination = ruleElement.getAttribute("destination");
                    defaultRules.put(operation, destination);
                }
            }

            // Load ID-based rules
            NodeList idBasedRuleNodes = document.getElementsByTagName("id-based-rules");
            if (idBasedRuleNodes.getLength() > 0) {
                Element idBasedRulesElement = (Element) idBasedRuleNodes.item(0);
                NodeList ruleNodes = idBasedRulesElement.getElementsByTagName("rule");
                
                for (int i = 0; i < ruleNodes.getLength(); i++) {
                    Element ruleElement = (Element) ruleNodes.item(i);
                    String condition = ruleElement.getAttribute("condition");
                    String destination = ruleElement.getAttribute("destination");
                    idBasedRules.put(condition, destination);
                }
            }

            System.out.println("[Config Reader] Loaded routing configuration successfully");
            System.out.println("[Config Reader] Default rules: " + defaultRules);
            System.out.println("[Config Reader] ID-based rules: " + idBasedRules);

        } catch (Exception e) {
            System.err.println("[Config Reader] Error loading routing configuration: " + e.getMessage());
            // Set fallback defaults
            defaultRules.put("CREATE", "verticle1");
            defaultRules.put("GET_ALL", "verticle2");
            idBasedRules.put("even", "verticle1");
            idBasedRules.put("odd", "verticle2");
            idBasedRules.put("null", "verticle1");
        }
    }

    private void loadVerticleConfigurations() {
        loadVerticleConfig("verticle1-config.xml", "verticle1");
        loadVerticleConfig("verticle2-config.xml", "verticle2");
    }

    private void loadVerticleConfig(String configFile, String verticleName) {
        try {
            ClassPathResource resource = new ClassPathResource(configFile);
            InputStream inputStream = resource.getInputStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            VerticleConfig config = new VerticleConfig();
            config.setName(verticleName);

            // Load supported operations
            NodeList operationNodes = document.getElementsByTagName("operation");
            for (int i = 0; i < operationNodes.getLength(); i++) {
                Element operationElement = (Element) operationNodes.item(i);
                String operationName = operationElement.getAttribute("name");
                boolean enabled = Boolean.parseBoolean(operationElement.getAttribute("enabled"));
                String priority = operationElement.getAttribute("priority");
                
                config.addSupportedOperation(operationName, enabled, priority);
            }

            // Load performance settings
            NodeList performanceNodes = document.getElementsByTagName("performance");
            if (performanceNodes.getLength() > 0) {
                Element performanceElement = (Element) performanceNodes.item(0);
                
                NodeList maxConcurrentNodes = performanceElement.getElementsByTagName("max-concurrent-operations");
                if (maxConcurrentNodes.getLength() > 0) {
                    int maxConcurrent = Integer.parseInt(maxConcurrentNodes.item(0).getTextContent());
                    config.setMaxConcurrentOperations(maxConcurrent);
                }
                
                NodeList timeoutNodes = performanceElement.getElementsByTagName("timeout-seconds");
                if (timeoutNodes.getLength() > 0) {
                    int timeout = Integer.parseInt(timeoutNodes.item(0).getTextContent());
                    config.setTimeoutSeconds(timeout);
                }
            }

            verticleConfigs.put(verticleName, config);
            System.out.println("[Config Reader] Loaded " + verticleName + " configuration: " + config);

        } catch (Exception e) {
            System.err.println("[Config Reader] Error loading " + configFile + ": " + e.getMessage());
        }
    }

    public String getDestinationForOperation(String operation, Long userId) {
        // Check default rules first
        if (defaultRules.containsKey(operation)) {
            return defaultRules.get(operation);
        }

        // Apply ID-based routing
        if (userId == null) {
            return idBasedRules.get("null");
        }

        String condition = (userId % 2 == 0) ? "even" : "odd";
        return idBasedRules.get(condition);
    }

    public boolean isOperationSupportedByVerticle(String verticleName, String operation) {
        VerticleConfig config = verticleConfigs.get(verticleName);
        return config != null && config.isOperationSupported(operation);
    }

    public VerticleConfig getVerticleConfig(String verticleName) {
        return verticleConfigs.get(verticleName);
    }

    public Map<String, String> getDefaultRules() {
        return defaultRules;
    }

    public Map<String, String> getIdBasedRules() {
        return idBasedRules;
    }

    // Inner class for Verticle Configuration
    public static class VerticleConfig {
        private String name;
        private Map<String, OperationConfig> supportedOperations = new HashMap<>();
        private int maxConcurrentOperations = 10;
        private int timeoutSeconds = 30;

        public void setName(String name) {
            this.name = name;
        }

        public void addSupportedOperation(String operation, boolean enabled, String priority) {
            supportedOperations.put(operation, new OperationConfig(enabled, priority));
        }

        public boolean isOperationSupported(String operation) {
            OperationConfig config = supportedOperations.get(operation);
            return config != null && config.isEnabled();
        }

        public void setMaxConcurrentOperations(int maxConcurrentOperations) {
            this.maxConcurrentOperations = maxConcurrentOperations;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxConcurrentOperations() {
            return maxConcurrentOperations;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        @Override
        public String toString() {
            return "VerticleConfig{" +
                    "name='" + name + '\'' +
                    ", supportedOperations=" + supportedOperations +
                    ", maxConcurrentOperations=" + maxConcurrentOperations +
                    ", timeoutSeconds=" + timeoutSeconds +
                    '}';
        }

        public static class OperationConfig {
            private boolean enabled;
            private String priority;

            public OperationConfig(boolean enabled, String priority) {
                this.enabled = enabled;
                this.priority = priority;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public String getPriority() {
                return priority;
            }

            @Override
            public String toString() {
                return "OperationConfig{" +
                        "enabled=" + enabled +
                        ", priority='" + priority + '\'' +
                        '}';
            }
        }
    }
}
