package com.example.crud.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class UserCamelRoute extends RouteBuilder {
    @Override
    public void configure() {
        from("kafka:user-topic?brokers=localhost:9092")
            .choice()
                .when(simple("${body} contains 'someCondition'"))
                    .to("vertx:verticle2")
                .otherwise()
                    .to("vertx:verticle3");
    }
}