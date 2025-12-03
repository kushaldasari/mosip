package com.example.crud.kafka;

import com.example.crud.model.User;
import com.example.crud.service.UserService;
import com.example.crud.kafka.UnifiedKafkaProducer.CrudOperation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

// DISABLED: All Kafka consumption now handled by Apache Camel Router
// @Component
public class UnifiedKafkaConsumer {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UnifiedKafkaConsumer(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
    }

    // Disabled: Only Camel Router will consume from crud-operations-topic
    // @KafkaListener(topics = "crud-operations-topic", groupId = "unified-consumer-group")
    public void consumeUnifiedOperations(ConsumerRecord<String, String> record) {
        String message = record.value();
        System.out.println("[Unified Kafka Consumer] Received: " + message);
        
        try {
            CrudOperation operation = objectMapper.readValue(message, CrudOperation.class);
            String operationType = operation.getOperation();
            Object data = operation.getData();
            
            System.out.println("[Unified Kafka Consumer] Processing " + operationType + " operation");
            
            switch (operationType) {
                case "CREATE":
                    handleCreateOperation(data);
                    break;
                case "UPDATE":
                    handleUpdateOperation(data);
                    break;
                case "READ":
                    handleReadOperation(data);
                    break;
                case "DELETE":
                    handleDeleteOperation(data);
                    break;
                case "GET_ALL":
                    handleGetAllOperation();
                    break;
                default:
                    System.err.println("[Unified Kafka Consumer] Unknown operation: " + operationType);
            }
            
        } catch (Exception e) {
            System.err.println("[Unified Kafka Consumer] Failed to process message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCreateOperation(Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            User user = objectMapper.readValue(jsonData, User.class);
            
            User savedUser = userService.createUser(user);
            System.out.println("[Unified Kafka Consumer] User created: " + savedUser.getName() + " (ID: " + savedUser.getId() + ")");
            
        } catch (Exception e) {
            System.err.println("[Unified Kafka Consumer] Error in CREATE operation: " + e.getMessage());
        }
    }

    private void handleUpdateOperation(Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            User user = objectMapper.readValue(jsonData, User.class);
            
            if (user.getId() != null) {
                User updatedUser = userService.updateUser(user.getId(), user);
                if (updatedUser != null) {
                    System.out.println("[Unified Kafka Consumer] User updated: " + updatedUser.getName() + " (ID: " + updatedUser.getId() + ")");
                } else {
                    System.err.println("[Unified Kafka Consumer] User not found for update with ID: " + user.getId());
                }
            } else {
                System.err.println("[Unified Kafka Consumer] User ID is required for UPDATE operation");
            }
            
        } catch (Exception e) {
            System.err.println("[Unified Kafka Consumer] Error in UPDATE operation: " + e.getMessage());
        }
    }

    private void handleReadOperation(Object data) {
        try {
            Long userId;
            
            if (data instanceof Number) {
                userId = ((Number) data).longValue();
            } else if (data instanceof String) {
                userId = Long.parseLong((String) data);
            } else {
                System.err.println("[Unified Kafka Consumer] Invalid data type for READ operation: " + data);
                return;
            }
            
            User user = userService.getUser(userId);
            if (user != null) {
                System.out.println("[Unified Kafka Consumer] User found: " + user.getName() + " (" + user.getEmail() + ")");
            } else {
                System.out.println("[Unified Kafka Consumer] User not found with ID: " + userId);
            }
            
        } catch (Exception e) {
            System.err.println("[Unified Kafka Consumer] Error in READ operation: " + e.getMessage());
        }
    }

    private void handleDeleteOperation(Object data) {
        try {
            Long userId;
            
            if (data instanceof Number) {
                userId = ((Number) data).longValue();
            } else if (data instanceof String) {
                userId = Long.parseLong((String) data);
            } else {
                System.err.println("[Unified Kafka Consumer] Invalid data type for DELETE operation: " + data);
                return;
            }
            
            boolean deleted = userService.deleteUser(userId);
            if (deleted) {
                System.out.println("[Unified Kafka Consumer] User deleted successfully with ID: " + userId);
            } else {
                System.out.println("[Unified Kafka Consumer] User not found for deletion with ID: " + userId);
            }
            
        } catch (Exception e) {
            System.err.println("[Unified Kafka Consumer] Error in DELETE operation: " + e.getMessage());
        }
    }

    private void handleGetAllOperation() {
        try {
            List<User> users = userService.getAllUsers();
            System.out.println("[Unified Kafka Consumer] Retrieved " + users.size() + " users:");
            
            for (User user : users) {
                System.out.println("  - ID: " + user.getId() + ", Name: " + user.getName() + ", Email: " + user.getEmail());
            }
            
        } catch (Exception e) {
            System.err.println("[Unified Kafka Consumer] Error in GET_ALL operation: " + e.getMessage());
        }
    }

    // Keep the original consumer for backward compatibility (DISABLED)
    // @KafkaListener(topics = "user-topic", groupId = "user-group")
    public void consumeLegacyUserTopic(ConsumerRecord<String, String> record) {
        String message = record.value();
        System.out.println("[Legacy Kafka Consumer] Received: " + message);
        try {
            User user = objectMapper.readValue(message, User.class);
            userService.createUser(user);
            System.out.println("[Legacy Kafka Consumer] User saved to DB: " + user);
        } catch (Exception e) {
            System.err.println("[Legacy Kafka Consumer] Failed to process message: " + e.getMessage());
        }
    }
}
