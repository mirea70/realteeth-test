package com.realteeth.workerclient.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class WorkerClientConfig {
    private final WorkerClientProperties properties;

    @Bean
    RestClient workerRestClient() {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
