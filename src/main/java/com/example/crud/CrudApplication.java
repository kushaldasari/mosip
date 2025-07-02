package com.example.crud;

import com.example.crud.service.UserService;
import com.example.crud.vertx.UserVerticle;
import com.example.crud.kafka.KafkaDeleteUserVerticle;
import io.vertx.core.Vertx;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class CrudApplication {

    private final Vertx vertx = Vertx.vertx();
    private final UserService userService;

    public CrudApplication(UserService userService) {
        this.userService = userService;
    }

    public static void main(String[] args) {
        SpringApplication.run(CrudApplication.class, args);
    }

    @PostConstruct
    public void deployVerticles() {
        vertx.deployVerticle(new UserVerticle(userService));
        vertx.deployVerticle(new KafkaDeleteUserVerticle(userService)); // Deploy Kafka consumer Verticle
    }

    @Bean
    public Vertx vertx() {
        return vertx;
    }
}
