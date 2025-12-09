package com.example.crud.vertx;

import com.example.crud.model.User;
import com.example.crud.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public class CrudVerticle1 extends AbstractVerticle {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public CrudVerticle1(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start() {
        System.out.println("[Verticle 1] Started and listening for ALL operations (Even IDs + CREATE) on 'verticle1.operations' channel.");
        
        vertx.eventBus().consumer("verticle1.operations", message -> {
            JsonObject request = (JsonObject) message.body();
            String operation = request.getString("operation");
            Object data = request.getValue("data");
            
            System.out.println("[Verticle 1] Received " + operation + " operation with data: " + data);
            
            try {
                switch (operation) {
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
                        System.err.println("[Verticle 1] Unknown operation: " + operation);
                }
            } catch (Exception e) {
                System.err.println("[Verticle 1] Error processing " + operation + " operation: " + e.getMessage());

                e.printStackTrace();
            }
        });
    }

    private void handleCreateOperation(Object data) {
        try {
            System.out.println("[Verticle 1] Raw data received: " + data);
            System.out.println("[Verticle 1] Data type: " + data.getClass().getName());
            
            User user = null;
            
            // Handle different data types with more robust parsing
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                
                // Check if it's a nested structure with "map" field
                if (dataMap.containsKey("map") && dataMap.get("map") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) dataMap.get("map");
                    user = objectMapper.convertValue(userMap, User.class);
                } else {
                    // Direct map conversion
                    user = objectMapper.convertValue(dataMap, User.class);
                }
            } else if (data instanceof String) {
                // Parse JSON string
                user = objectMapper.readValue((String) data, User.class);
            } else {
                // Try to extract user data from complex object
                String jsonData = objectMapper.writeValueAsString(data);
                System.out.println("[Verticle 1] JSON representation: " + jsonData);
                
                // Try to parse as JsonNode first to inspect structure
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(jsonData);
                if (jsonNode.has("map")) {
                    user = objectMapper.treeToValue(jsonNode.get("map"), User.class);
                } else {
                    user = objectMapper.treeToValue(jsonNode, User.class);
                }
            }
            
            if (user != null) {
                // CRITICAL: Remove ID for CREATE operation to avoid Hibernate confusion
                Long originalId = user.getId();
                user.setId(null);
                
                System.out.println("[Verticle 1] Processing CREATE for user: " + user.getName() + 
                                 " (original ID: " + originalId + " -> cleared for CREATE)");
                
                User savedUser = userService.createUser(user);
                System.out.println("[Verticle 1] User created successfully with new ID: " + savedUser.getId());
            } else {
                System.err.println("[Verticle 1] Failed to parse user data");
            }
            
        } catch (Exception e) {
            System.out.println(data);
            System.err.println("[Verticle 1] Error in CREATE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUpdateOperation(Object data) {
        try {
            System.out.println("[Verticle 1] Raw UPDATE data received: " + data);
            
            User user = null;
            
            // Handle different data types with robust parsing
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                
                // Check if it's a nested structure with "map" field
                if (dataMap.containsKey("map") && dataMap.get("map") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) dataMap.get("map");
                    user = objectMapper.convertValue(userMap, User.class);
                } else {
                    // Direct map conversion
                    user = objectMapper.convertValue(dataMap, User.class);
                }
            } else if (data instanceof String) {
                // Parse JSON string
                user = objectMapper.readValue((String) data, User.class);
            } else {
                // Try to extract user data from complex object
                String jsonData = objectMapper.writeValueAsString(data);
                System.out.println("[Verticle 1] UPDATE JSON representation: " + jsonData);
                
                JsonNode jsonNode = objectMapper.readTree(jsonData);
                if (jsonNode.has("map")) {
                    user = objectMapper.treeToValue(jsonNode.get("map"), User.class);
                } else {
                    user = objectMapper.treeToValue(jsonNode, User.class);
                }
            }
            
            if (user != null) {
                System.out.println("[Verticle 1] Processing UPDATE for user ID: " + user.getId());
                
                if (user.getId() != null) {
                    User updatedUser = userService.updateUser(user.getId(), user);
                    if (updatedUser != null) {
                        System.out.println("[Verticle 1] User updated successfully: " + updatedUser.getName());
                    } else {
                        System.err.println("[Verticle 1] User not found for update with ID: " + user.getId());
                    }
                } else {
                    System.err.println("[Verticle 1] User ID is required for UPDATE operation");
                }
            } else {
                System.err.println("[Verticle 1] Failed to parse UPDATE user data");
            }
            
        } catch (Exception e) {
            System.err.println("[Verticle 1] Error in UPDATE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleReadOperation(Object data) {
        try {
            Long userId;
            
            // Handle different data types for user ID
            if (data instanceof Number) {
                userId = ((Number) data).longValue();
            } else if (data instanceof String) {
                userId = Long.parseLong((String) data);
            } else {
                System.err.println("[Verticle 1] Invalid data type for READ operation: " + data);
                return;
            }
            
            System.out.println("[Verticle 1] Processing READ for user ID: " + userId);
            User user = userService.getUser(userId);
            if (user != null) {
                System.out.println("[Verticle 1] User found: " + user.getName() + " (" + user.getEmail() + ")");
            } else {
                System.out.println("[Verticle 1] User not found with ID: " + userId);
            }
            
        } catch (Exception e) {
            System.err.println("[Verticle 1] Error in READ operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDeleteOperation(Object data) {
        try {
            Long userId;
            
            // Handle different data types for user ID
            if (data instanceof Number) {
                userId = ((Number) data).longValue();
            } else if (data instanceof String) {
                userId = Long.parseLong((String) data);
            } else {
                System.err.println("[Verticle 1] Invalid data type for DELETE operation: " + data);
                return;
            }
            
            System.out.println("[Verticle 1] Processing DELETE for user ID: " + userId);
            boolean deleted = userService.deleteUser(userId);
            if (deleted) {
                System.out.println("[Verticle 1] User deleted successfully with ID: " + userId);
            } else {
                System.out.println("[Verticle 1] User not found for deletion with ID: " + userId);
            }
            
        } catch (Exception e) {
            System.err.println("[Verticle 1] Error in DELETE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGetAllOperation() {
        try {
            List<User> users = userService.getAllUsers();
            System.out.println("[Verticle 1] Retrieved " + users.size() + " users:");
            
            for (User user : users) {
                System.out.println("  - ID: " + user.getId() + ", Name: " + user.getName() + ", Email: " + user.getEmail());
            }
            
        } catch (Exception e) {
            System.err.println("[Verticle 1] Error in GET_ALL operation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
