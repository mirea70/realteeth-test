package com.realteeth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RealteethWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RealteethWorkerApplication.class, args);
    }
}
