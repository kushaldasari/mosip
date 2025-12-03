package com.example.crud.camel;

import com.example.crud.kafka.UnifiedKafkaProducer.CrudOperation;
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
                    
                    // Set headers for routing decisions
                    exchange.getIn().setHeader("operationType", operationType);
                    exchange.getIn().setHeader("operationData", operation.getData());
                    
                    System.out.println("[Camel Bridge] Operation type: " + operationType);
                    
                } catch (Exception e) {
                    System.err.println("[Camel Bridge] Error parsing message: " + e.getMessage());
                    throw e;
                }
            })
            .choice()
                .when(header("operationType").in("CREATE", "UPDATE"))
                    .log("Routing ${header.operationType} to Verticle 1 (CREATE/UPDATE)")
                    .to("direct:verticle1")
                .when(header("operationType").in("READ", "DELETE", "GET_ALL"))
                    .log("Routing ${header.operationType} to Verticle 2 (READ/DELETE)")
                    .to("direct:verticle2")
                .otherwise()
                    .log("Unknown operation type: ${header.operationType}")
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
}
