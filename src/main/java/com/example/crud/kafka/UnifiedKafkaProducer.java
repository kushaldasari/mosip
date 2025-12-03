package com.example.crud.kafka;

import com.example.crud.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UnifiedKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String UNIFIED_TOPIC = "crud-operations-topic";

    public UnifiedKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void sendOperation(String operation, Object data) {
        try {
            CrudOperation crudOperation = new CrudOperation(operation, data);
            String json = objectMapper.writeValueAsString(crudOperation);
            kafkaTemplate.send(UNIFIED_TOPIC, json);
            System.out.println("[Unified Kafka Producer] Sent " + operation + " operation to topic: " + json);
        } catch (JsonProcessingException e) {
            System.err.println("[Unified Kafka Producer] Failed to serialize operation: " + e.getMessage());
        }
    }

    public void sendCreateOperation(User user) {
        sendOperation("CREATE", user);
    }

    public void sendUpdateOperation(User user) {
        sendOperation("UPDATE", user);
    }

    public void sendReadOperation(Long userId) {
        sendOperation("READ", userId);
    }

    public void sendDeleteOperation(Long userId) {
        sendOperation("DELETE", userId);
    }

    public void sendGetAllOperation() {
        sendOperation("GET_ALL", null);
    }

    // Inner class to represent CRUD operation structure
    public static class CrudOperation {
        private String operation;
        private Object data;

        public CrudOperation() {}

        public CrudOperation(String operation, Object data) {
            this.operation = operation;
            this.data = data;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
