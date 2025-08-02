package ru.practicum.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventShortDto;

import java.util.List;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/events/{id}")
    EventFullDto getEventById(@PathVariable Long id);

    @GetMapping("/events/{id}/short")
    EventShortDto getEventShort(@PathVariable Long id);

    @GetMapping("/events")
    List<EventFullDto> findAllById(@RequestParam List<Long> ids);

    @GetMapping("/events/short")
    List<EventShortDto> getEventsShortDto(@RequestParam List<Long> ids);

    @PostMapping("/events/{id}/confirmed")
    void updateConfirmedRequests(@PathVariable("id") Long eventId, @RequestParam int delta);

}
