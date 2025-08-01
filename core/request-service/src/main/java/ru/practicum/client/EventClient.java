package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.event.EventFullDto;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/events/{id}")
    EventFullDto getEventById(@PathVariable("id") Long eventId);

    @PatchMapping("/events/{id}/confirmed")
    void updateConfirmedRequests(@PathVariable("id") Long eventId, @RequestParam int delta);
}