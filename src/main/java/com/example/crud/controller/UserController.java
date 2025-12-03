package com.example.crud.controller;

import com.example.crud.kafka.UnifiedKafkaProducer;
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
    private final UnifiedKafkaProducer unifiedKafkaProducer;
    private final UserService userService;
    private final Vertx vertx;

    @Autowired
    public UserController(UserService userService, Vertx vertx, UnifiedKafkaProducer unifiedKafkaProducer) {
        this.userService = userService;
        this.vertx = vertx;
        this.unifiedKafkaProducer = unifiedKafkaProducer;
    }

    // 🔹 Basic CRUD operations
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

    // 🔹 Vert.x EventBus Async Get
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

    // 🔹 Unified Kafka Operations - Complete Flow: Client → Kafka → Camel → Verticles
    
    @PostMapping("/kafka/create")
    public String createUserViaKafka(@RequestBody User user) {
        unifiedKafkaProducer.sendCreateOperation(user);
        return "CREATE operation sent to Kafka → Camel → Verticle 1 successfully!";
    }

    @PutMapping("/kafka/update")
    public String updateUserViaKafka(@RequestBody User user) {
        unifiedKafkaProducer.sendUpdateOperation(user);
        return "UPDATE operation sent to Kafka → Camel → Verticle 1 successfully!";
    }

    @GetMapping("/kafka/read/{id}")
    public String readUserViaKafka(@PathVariable Long id) {
        unifiedKafkaProducer.sendReadOperation(id);
        return "READ operation sent to Kafka → Camel → Verticle 2 successfully!";
    }

    @DeleteMapping("/kafka/delete/{id}")
    public String deleteUserViaKafka(@PathVariable Long id) {
        unifiedKafkaProducer.sendDeleteOperation(id);
        return "DELETE operation sent to Kafka → Camel → Verticle 2 successfully!";
    }

    @GetMapping("/kafka/all")
    public String getAllUsersViaKafka() {
        unifiedKafkaProducer.sendGetAllOperation();
        return "GET_ALL operation sent to Kafka → Camel → Verticle 2 successfully!";
    }
    
}
