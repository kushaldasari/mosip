package com.example.crud.Verticles;

import io.vertx.core.AbstractVerticle;

public class Verticle3 extends AbstractVerticle {

    @Override
    public void start() {
        vertx.eventBus().consumer("event.type2", message -> {
            System.out.println("[Verticle3] Received from Camel: " + message.body());
            // Handle Type2 logic here
        });
    }
}
