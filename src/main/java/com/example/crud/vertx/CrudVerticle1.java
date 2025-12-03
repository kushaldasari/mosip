package com.example.crud.vertx;

import com.example.crud.model.User;
import com.example.crud.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class CrudVerticle1 extends AbstractVerticle {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public CrudVerticle1(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start() {
        System.out.println("[Verticle 1] Started and listening for CREATE/UPDATE operations on 'verticle1.operations' channel.");
        
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
            // Convert data to User object
            String jsonData = objectMapper.writeValueAsString(data);
            User user = objectMapper.readValue(jsonData, User.class);
            
            System.out.println("[Verticle 1] Processing CREATE for user: " + user.getName());
            
            User savedUser = userService.createUser(user);
            System.out.println("[Verticle 1] User created successfully with ID: " + savedUser.getId());
            
        } catch (Exception e) {
            System.err.println("[Verticle 1] Error in CREATE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUpdateOperation(Object data) {
        try {
            // Convert data to User object
            String jsonData = objectMapper.writeValueAsString(data);
            User user = objectMapper.readValue(jsonData, User.class);
            
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
            
        } catch (Exception e) {
            System.err.println("[Verticle 1] Error in UPDATE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
