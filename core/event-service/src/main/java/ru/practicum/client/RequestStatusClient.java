package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.request.RequestStatus;

@FeignClient(name = "request-service-status")
public interface RequestStatusClient {

    @GetMapping("/statuses/{name}")
    RequestStatus getStatusByName(@PathVariable("name") String name);
}