package com.example.crud.Verticles;

import io.vertx.core.AbstractVerticle;

public class Verticle2 extends AbstractVerticle {

    @Override
    public void start() {
        vertx.eventBus().consumer("event.type1", message -> {
            System.out.println("[Verticle2] Received from Camel: " + message.body());
            // Handle Type1 logic here
        });
    }
}
