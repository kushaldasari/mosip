package com.example.crud.camel;

import com.example.crud.kafka.UnifiedKafkaProducer.CrudOperation;
import com.example.crud.config.RoutingConfigurationReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CrudOperationRouter extends RouteBuilder {

    @Autowired
    private Vertx vertx;

    @Autowired
    private RoutingConfigurationReader routingConfigReader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure() throws Exception {
        
        // Main route: Consume from unified Kafka topic and route based on operation type
        from("kafka:crud-operations-topic?brokers=localhost:9092&groupId=camel-router-group")
            .log("Camel Bridge received message: ${body}")
            .process(exchange -> {
                String messageBody = exchange.getIn().getBody(String.class);
                System.out.println("[Camel Bridge] Processing message: " + messageBody);
                
                try {
                    CrudOperation operation = objectMapper.readValue(messageBody, CrudOperation.class);
                    String operationType = operation.getOperation();
                    Object operationData = operation.getData();
                    
                    // Set headers for routing decisions
                    exchange.getIn().setHeader("operationType", operationType);
                    exchange.getIn().setHeader("operationData", operationData);
                    
                    // Extract ID for routing logic
                    Long userId = extractUserId(operationType, operationData);
                    exchange.getIn().setHeader("userId", userId);
                    
                    // Determine routing based on XML configuration
                    String routingDestination = routingConfigReader.getDestinationForOperation(operationType, userId);
                    exchange.getIn().setHeader("routingDestination", routingDestination);
                    
                    // Validate if operation is supported by target verticle
                    boolean isSupported = routingConfigReader.isOperationSupportedByVerticle(routingDestination, operationType);
                    exchange.getIn().setHeader("operationSupported", isSupported);
                    
                    System.out.println("[Camel Bridge] Operation: " + operationType + 
                                     ", User ID: " + userId + 
                                     ", Routing to: " + routingDestination + 
                                     ", Supported: " + isSupported);
                    
                } catch (Exception e) {
                    System.err.println("[Camel Bridge] Error parsing message: " + e.getMessage());
                    throw e;
                }
            })
            .process(exchange -> {
                // Apply fallback routing if operation not supported
                String routingDestination = exchange.getIn().getHeader("routingDestination", String.class);
                boolean operationSupported = exchange.getIn().getHeader("operationSupported", Boolean.class);
                
                if (!operationSupported) {
                    String fallbackDestination = "verticle1".equals(routingDestination) ? "verticle2" : "verticle1";
                    exchange.getIn().setHeader("routingDestination", fallbackDestination);
                    System.out.println("[Camel Bridge] Operation not supported, fallback routing to: " + fallbackDestination);
                }
            })
            .choice()
                .when(header("routingDestination").isEqualTo("verticle1"))
                    .log("Routing ${header.operationType} (ID: ${header.userId}) to Verticle 1 [XML Config]")
                    .to("direct:verticle1")
                .when(header("routingDestination").isEqualTo("verticle2"))
                    .log("Routing ${header.operationType} (ID: ${header.userId}) to Verticle 2 [XML Config]")
                    .to("direct:verticle2")
                .otherwise()
                    .log("ERROR: Unknown routing destination: ${header.routingDestination}, defaulting to Verticle 1")
                    .to("direct:verticle1")
            .end();

        // Route to Verticle 1 (CREATE/UPDATE operations)
        from("direct:verticle1")
            .log("Sending to Verticle 1: ${header.operationType}")
            .process(exchange -> {
                String operationType = exchange.getIn().getHeader("operationType", String.class);
                Object operationData = exchange.getIn().getHeader("operationData");
                
                JsonObject message = new JsonObject()
                    .put("operation", operationType)
                    .put("data", operationData);
                
                // Send to Vert.x EventBus for Verticle 1
                vertx.eventBus().send("verticle1.operations", message);
                System.out.println("[Camel Bridge] Sent to Verticle 1: " + message);
            });

        // Route to Verticle 2 (READ/DELETE operations)
        from("direct:verticle2")
            .log("Sending to Verticle 2: ${header.operationType}")
            .process(exchange -> {
                String operationType = exchange.getIn().getHeader("operationType", String.class);
                Object operationData = exchange.getIn().getHeader("operationData");
                
                JsonObject message = new JsonObject()
                    .put("operation", operationType)
                    .put("data", operationData);
                
                // Send to Vert.x EventBus for Verticle 2
                vertx.eventBus().send("verticle2.operations", message);
                System.out.println("[Camel Bridge] Sent to Verticle 2: " + message);
            });
    }
    
    /**
     * Extract User ID from operation data for routing decisions
     */
    private Long extractUserId(String operationType, Object operationData) {
        try {
            switch (operationType) {
                case "CREATE":
                    // CREATE operations don't have ID initially, return null
                    return null;
                    
                case "UPDATE":
                    // UPDATE operations should have user object with ID
                    if (operationData instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) operationData;
                        Object idObj = userMap.get("id");
                        if (idObj instanceof Number) {
                            return ((Number) idObj).longValue();
                        }
                    }
                    return null;
                    
                case "READ":
                case "DELETE":
                    // READ/DELETE operations have ID as data
                    if (operationData instanceof Number) {
                        return ((Number) operationData).longValue();
                    } else if (operationData instanceof String) {
                        return Long.parseLong((String) operationData);
                    }
                    return null;
                    
                case "GET_ALL":
                    // GET_ALL doesn't have specific ID
                    return null;
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("[Camel Bridge] Error extracting user ID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Routing logic is now handled by XML configuration files:
     * - routing-rules.xml: Contains default and ID-based routing rules
     * - verticle1-config.xml: Configuration for Verticle 1
     * - verticle2-config.xml: Configuration for Verticle 2
     * 
     * The RoutingConfigurationReader loads these XML files and provides
     * the routing decisions dynamically.
     */
}
