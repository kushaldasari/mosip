package com.example.crud.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.annotations.Component;
import org.springframework.beans.factory.annotation.Autowired;

import io.vertx.core.Vertx;

@Component(value = "vertx")
public class CamelKafkaRouter extends RouteBuilder {

    @Autowired
    private Vertx vertx;

    @Override
    public void configure() {
        from("kafka:event-topic?brokers=localhost:9092")
            .routeId("event-router")
            .log("Camel received: ${body}")
            .choice()
                .when(simple("${body} contains 'type1'"))
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        vertx.eventBus().send("event.type1", body);
                    })
                .otherwise()
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        vertx.eventBus().send("event.type2", body);
                    });
    }
}
