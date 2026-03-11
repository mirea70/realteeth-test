package com.realteeth.workerclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker")
public record WorkerClientProperties(
        String baseUrl,
        String candidateName,
        String email
) {}
