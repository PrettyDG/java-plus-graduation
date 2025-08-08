package ru.practicum.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;

import java.util.List;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/requests/event/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventId(@PathVariable("eventId") Long eventId);

    @GetMapping("/requests")
    List<ParticipationRequestDto> getRequestsByIds(@RequestParam("ids") List<Long> ids);

    @PostMapping("/requests/save-all")
    List<ParticipationRequestDto> saveAll(@RequestBody List<ParticipationRequestDto> requests);

    @GetMapping("/requests/statuses/{name}")
    RequestStatus getStatusByName(@PathVariable("name") String name);

    @GetMapping("/exist/{eventId}/{userId}")
    boolean isRequestExist(@PathVariable Long eventId,
                           @PathVariable Long userId);
}