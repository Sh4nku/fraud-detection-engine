package com.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableScheduling
public class FraudDetectionEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionEngineApplication.class, args);
    }
}
