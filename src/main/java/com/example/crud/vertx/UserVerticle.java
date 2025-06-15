package com.example.crud.vertx;

import com.example.crud.model.User;
import com.example.crud.service.UserService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class UserVerticle extends AbstractVerticle {

    private final UserService userService;

    public UserVerticle(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void start() {
        System.out.println("[Vert.x] UserVerticle started and listening on 'user.get' channel.");
    
        vertx.eventBus().consumer("user.get", message -> {
            JsonObject request = (JsonObject) message.body();
            Long id = request.getLong("id");
    
            System.out.println("[Vert.x] Received request for user id: " + id + " on thread: " + Thread.currentThread().getName());
    
            User user = userService.getUser(id);
    
            if (user != null) {
                System.out.println("[Vert.x] Found user: " + user.getName() + ", replying back.");
    
                JsonObject response = new JsonObject()
                        .put("id", user.getId())
                        .put("name", user.getName())
                        .put("email", user.getEmail());
    
                message.reply(response);
            } else {
                System.err.println("[Vert.x] User not found with id: " + id);
                message.fail(404, "User not found");
            }
        });
    }
    
}
