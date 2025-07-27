package com.example.crud.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.spi.annotations.Component;
import org.apache.kafka.clients.producer.ProducerConfig;

import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

@Component(value = "vertx")
public class KafkaEventProducer {

    private final KafkaProducer<String, String> producer;

    public KafkaEventProducer(Vertx vertx) {
        Map<String, String> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = KafkaProducer.create(vertx, config);
    }

    public void sendEvent(String payload) {
        KafkaProducerRecord<String, String> record = KafkaProducerRecord.create("event-topic", payload);
        producer.send(record, res -> {
            if (res.succeeded()) {
                System.out.println("[KafkaProducer] Sent event: " + payload);
            } else {
                System.err.println("[KafkaProducer] Failed: " + res.cause());
            }
        });
    }
}

