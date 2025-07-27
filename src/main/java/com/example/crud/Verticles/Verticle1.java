package com.example.crud.Verticles;

import com.example.crud.kafka.KafkaEventProducer;

import io.vertx.core.AbstractVerticle;

public class Verticle1 extends AbstractVerticle {

    private final KafkaEventProducer kafkaEventProducer;

    public Verticle1(KafkaEventProducer kafkaEventProducer) {
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Override
    public void start() {
        vertx.eventBus().consumer("event.create", message -> {
            String payload = (String) message.body();
            System.out.println("[Verticle1] Received: " + payload);
            kafkaEventProducer.sendEvent(payload); // 🔄 Send to Kafka
        });
    }
}
