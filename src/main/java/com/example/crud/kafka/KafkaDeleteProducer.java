package com.example.crud.kafka;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.stereotype.Component;

@Component
public class KafkaDeleteProducer {

    private final KafkaProducer<String, String> producer;

    public KafkaDeleteProducer(Vertx vertx) {
        Map<String, String> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = KafkaProducer.create(vertx, config);
    }

    public void sendDeleteUser(Long userId) {
        KafkaProducerRecord<String, String> record =
                KafkaProducerRecord.create("delete-user-topic", String.valueOf(userId));

        producer.send(record, done -> {
            if (done.succeeded()) {
                System.out.println("[KafkaProducer] Sent delete request for user ID: " + userId);
            } else {
                System.err.println("[KafkaProducer] Failed to send delete request: " + done.cause());
            }
        });
    }
}
