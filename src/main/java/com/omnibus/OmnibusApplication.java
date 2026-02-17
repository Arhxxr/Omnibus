package com.Omnibus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OmnibusApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmnibusApplication.class, args);
    }
}
