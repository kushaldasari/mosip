package com.example.crud.vertx;

import com.example.crud.model.User;
import com.example.crud.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class CrudVerticle2 extends AbstractVerticle {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public CrudVerticle2(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start() {
        System.out.println("[Verticle 2] Started and listening for READ/DELETE operations on 'verticle2.operations' channel.");
        
        vertx.eventBus().consumer("verticle2.operations", message -> {
            JsonObject request = (JsonObject) message.body();
            String operation = request.getString("operation");
            Object data = request.getValue("data");
            
            System.out.println("[Verticle 2] Received " + operation + " operation with data: " + data);
            
            try {
                switch (operation) {
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
                        System.err.println("[Verticle 2] Unknown operation: " + operation);
                }
            } catch (Exception e) {
                System.err.println("[Verticle 2] Error processing " + operation + " operation: " + e.getMessage());
                e.printStackTrace();
            }
        });
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
                System.err.println("[Verticle 2] Invalid data type for READ operation: " + data);
                return;
            }
            
            System.out.println("[Verticle 2] Processing READ for user ID: " + userId);
            
            User user = userService.getUser(userId);
            if (user != null) {
                System.out.println("[Verticle 2] User found: " + user.getName() + " (" + user.getEmail() + ")");
            } else {
                System.out.println("[Verticle 2] User not found with ID: " + userId);
            }
            
        } catch (Exception e) {
            System.err.println("[Verticle 2] Error in READ operation: " + e.getMessage());
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
                System.err.println("[Verticle 2] Invalid data type for DELETE operation: " + data);
                return;
            }
            
            System.out.println("[Verticle 2] Processing DELETE for user ID: " + userId);
            
            boolean deleted = userService.deleteUser(userId);
            if (deleted) {
                System.out.println("[Verticle 2] User deleted successfully with ID: " + userId);
            } else {
                System.out.println("[Verticle 2] User not found for deletion with ID: " + userId);
            }
            
        } catch (Exception e) {
            System.err.println("[Verticle 2] Error in DELETE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleGetAllOperation() {
        try {
            System.out.println("[Verticle 2] Processing GET_ALL operation");
            
            List<User> users = userService.getAllUsers();
            System.out.println("[Verticle 2] Retrieved " + users.size() + " users:");
            
            for (User user : users) {
                System.out.println("  - ID: " + user.getId() + ", Name: " + user.getName() + ", Email: " + user.getEmail());
            }
            
        } catch (Exception e) {
            System.err.println("[Verticle 2] Error in GET_ALL operation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
