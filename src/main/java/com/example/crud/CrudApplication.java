package com.example.crud;

import com.example.crud.service.UserService;
import com.example.crud.vertx.UserVerticle;
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
    public void deployVerticle() {
        vertx.deployVerticle(new UserVerticle(userService));
    }

    @Bean
    public Vertx vertx() {
        return vertx;
    }
}
