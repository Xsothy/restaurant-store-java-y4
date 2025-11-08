package com.restaurant.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestaurantStoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantStoreApiApplication.class, args);
    }

}