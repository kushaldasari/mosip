// UserController.java
package com.example.crud.controller;

import com.example.crud.kafka.UserKafkaProducer;
import com.example.crud.model.User;
import com.example.crud.service.UserService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserKafkaProducer userKafkaProducer;
    private final UserService userService;
    private final Vertx vertx;

    // private final UserKafkaProducer userKafkaProducer;

    @Autowired
    public UserController(UserService userService, Vertx vertx, UserKafkaProducer userKafkaProducer) {
        this.userService = userService;
        this.vertx = vertx;
        this.userKafkaProducer = userKafkaProducer;
    }
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id) ? "Deleted" : "Not Found";
    }
    @GetMapping("/async/{id}")
    public CompletableFuture<User> getUserAsync(@PathVariable Long id) {
        System.out.println("[Controller] Sending request to Vert.x EventBus for user id: " + id);
    
        CompletableFuture<User> future = new CompletableFuture<>();
        JsonObject request = new JsonObject().put("id", id);
    
        vertx.eventBus().request("user.get", request, reply -> {
            if (reply.succeeded()) {
                JsonObject result = (JsonObject) reply.result().body();
    
                System.out.println("[Controller] Received async reply from Vert.x: " + result);
    
                User user = new User();
                user.setId(result.getLong("id"));
                user.setName(result.getString("name"));
                user.setEmail(result.getString("email"));
                future.complete(user);
            } else {
                System.err.println("[Controller] Failed to get user from Vert.x: " + reply.cause().getMessage());
                future.completeExceptionally(reply.cause());
            }
        });
    
        return future;
    }

    @PostMapping("/kafka-create")
public String createUserKafka(@RequestBody User user) {
    userKafkaProducer.sendUser(user);
    return "User sent to Kafka successfully!";
}

    
}
