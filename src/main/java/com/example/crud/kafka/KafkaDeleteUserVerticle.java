package com.example.crud.kafka;

import com.example.crud.service.UserService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import io.vertx.kafka.client.consumer.KafkaConsumer;

import java.util.HashMap;
import java.util.Map;

public class KafkaDeleteUserVerticle extends AbstractVerticle {

    private final UserService userService;

    public KafkaDeleteUserVerticle(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void start() {
        Map<String, String> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "delete-user-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put("enable.auto.commit", "true");

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);

        consumer.handler(record -> {
            Long userId = Long.valueOf(record.value());
            boolean deleted = userService.deleteUser(userId);
            System.out.println("[KafkaConsumer] User delete request received for ID: " + userId + ", deleted: " + deleted);
        });

        consumer.subscribe("delete-user-topic");
    }
}
