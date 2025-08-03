package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Validated
public class RequestReceiveController {
    private final RequestService requestService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ParticipationRequestDto>> getRequestsByEventId(@PathVariable Long eventId) {
        List<ParticipationRequestDto> requests = requestService.findByEventId(eventId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getRequestsByIds(@RequestParam("ids") List<Long> ids) {
        List<ParticipationRequestDto> requests = requestService.findByIds(ids);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/save-all")
    public ResponseEntity<List<ParticipationRequestDto>> saveAll(@RequestBody List<ParticipationRequestDto> requests) {
        List<ParticipationRequestDto> saved = requestService.saveAll(requests);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/statuses/{name}")
    public RequestStatus getStatusByName(@PathVariable("name") String name) {
        return requestService.getStatusByName(name);
    }
}
