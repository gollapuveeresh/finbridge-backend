package com.finbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableSpringDataWebSupport
public class FinBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinBridgeApplication.class, args);
    }
}
