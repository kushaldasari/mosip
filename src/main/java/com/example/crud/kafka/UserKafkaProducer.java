package com.example.crud.kafka;

import com.example.crud.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void sendUser(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            kafkaTemplate.send("user-topic", json);
            System.out.println("[Kafka Producer] Sent user to topic: " + json);
        } catch (JsonProcessingException e) {
            System.err.println("[Kafka Producer] Failed to serialize user: " + e.getMessage());
        }
    }
}
