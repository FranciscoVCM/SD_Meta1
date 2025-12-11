package com.googol.webserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class WebServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebServerApplication.class, args);
    }
}

