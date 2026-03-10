package com.realteeth.mq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.rabbitmq")
public record RabbitMqProperties (
        String host,
        int port,
        String username,
        String password
){}
