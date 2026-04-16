package com.rescuehub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RescueHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(RescueHubApplication.class, args);
    }
}
