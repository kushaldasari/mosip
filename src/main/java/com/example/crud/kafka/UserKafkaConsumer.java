package com.example.crud.kafka;

import com.example.crud.model.User;
import com.example.crud.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserKafkaConsumer {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserKafkaConsumer(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "user-topic", groupId = "user-group")
    public void consume(ConsumerRecord<String, String> record) {
        String message = record.value();
        System.out.println("[Kafka Consumer] Received: " + message);
        try {
            User user = objectMapper.readValue(message, User.class);
            userService.createUser(user);
            System.out.println("[Kafka Consumer] User saved to DB: " + user);
        } catch (Exception e) {
            System.err.println("[Kafka Consumer] Failed to process message: " + e.getMessage());
        }
    }
}
