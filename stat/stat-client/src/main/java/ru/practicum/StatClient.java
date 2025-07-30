package ru.practicum;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.ViewStatsDto;

import java.net.URI;
import java.util.List;

public class StatClient {
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    protected final RestClient restClient;

    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";

    public StatClient(DiscoveryClient discoveryClient, String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;

        this.retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        this.restClient = RestClient.builder()
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .defaultStatusHandler(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) -> {
                            throw new RestClientException("HTTP error: " + response.getStatusText());
                        })
                .build();
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            if (instances == null || instances.isEmpty()) {
                throw new RestClientException("No instances found for service: " + statsServiceId);
            }
            return instances.get(0);
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId, exception);
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    public void saveStatEvent(EndpointHitDto endpointHitDto) {
        URI uri = makeUri(HIT_ENDPOINT);
        restClient.post()
                .uri(uri)
                .body(endpointHitDto)
                .retrieve()
                .toBodilessEntity();
    }

    public ResponseEntity<List<ViewStatsDto>> getStats(String start,
                                                       String end,
                                                       List<String> uris,
                                                       boolean unique) {
        String path = buildStatsUri(start, end, uris, unique);
        URI uri = makeUri(path);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    private String buildStatsUri(String start, String end, @Nullable List<String> uris, boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(STATS_ENDPOINT)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }

        return builder.build().toUriString();
    }
}
