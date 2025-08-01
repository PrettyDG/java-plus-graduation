package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatsClientConfig {

    @Value("${stat-server.service-id}")
    private String statsServiceId;

    private final DiscoveryClient discoveryClient;

    public StatsClientConfig(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Bean
    public StatClient statClient() {
        return new StatClient(discoveryClient, statsServiceId);
    }
}

