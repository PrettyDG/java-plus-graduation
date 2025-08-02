package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    List<EventFullDto> findAllById(@RequestParam("ids") List<Long> ids);
}
