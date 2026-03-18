package io.holbein.ephor.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EphorApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(EphorApiApplication.class, args);
    }
}
