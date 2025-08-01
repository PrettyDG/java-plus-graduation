package ru.practicum;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatClient {
    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    protected final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

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
            System.out.println("Найденные инстанты для " + statsServiceId + ": " + instances);

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
        return UriComponentsBuilder
                .newInstance()
                .scheme("http")
                .host(instance.getHost())
                .port(instance.getPort())
                .path(path)
                .build(true)
                .toUri();
    }

    private URI makeUri(String path, Map<String, Object> queryParams) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());

        UriComponentsBuilder builder = UriComponentsBuilder
                .newInstance()
                .scheme("http")
                .host(instance.getHost())
                .port(instance.getPort())
                .path(path);

        queryParams.forEach(builder::queryParam);

        return builder.build(true).toUri();
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
        Map<String, Object> queryParams = new LinkedHashMap<>();

        queryParams.put("start", formatDateTime(start));
        queryParams.put("end", formatDateTime(end));
        queryParams.put("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            queryParams.put("uris", String.join(",", uris));
        }

        URI uri = makeUri(STATS_ENDPOINT, queryParams);

        System.out.println("Запрос к Stats URI: " + uri);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    private String formatDateTime(String dateTime) {
        if (dateTime.contains("T")) {
            return dateTime;
        }


        try {
            LocalDateTime ldt = LocalDateTime.parse(dateTime.replace(" ", "T"));
            return ldt.format(FORMATTER);
        } catch (Exception e) {
            System.err.println("Warning: неверный формат даты-времени: " + dateTime);
            return dateTime.replace(" ", "T");
        }
    }
}
