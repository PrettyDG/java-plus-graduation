package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.AnalyzerClient;
import ru.practicum.CollectorClient;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventRecommend;
import ru.practicum.event.EventShortDto;
import ru.practicum.event.SearchPublicEventsParamDto;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.model.EventSort;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class PublicEventController {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_TEXT = "";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_START = 0;
    private static final int START_SEARCH_DATE_PERIOD = 100;
    private static final int END_SEARCH_DATE_PERIOD = 300;
    private final EventService eventService;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    @GetMapping("/recommendations")
    public List<EventRecommend> getRecommendations(@RequestHeader("X-EWM-USER-ID") long userId,
                                                   @RequestParam(defaultValue = "10") int maxResults) {
        var recommendationList = analyzerClient.getRecommendations(userId, maxResults).toList();

        List<EventRecommend> eventRecommends = new ArrayList<>();

        for (RecommendedEventProto eventProto : recommendationList) {
            eventRecommends.add(new EventRecommend(eventProto.getEventId(), eventProto.getScore()));
        }
        return eventRecommends;
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader("X-EWM-USER-ID") long userId) {
        eventService.addLike(userId, eventId);

        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    @GetMapping("/exists-by-category")
    public boolean existsByCategoryId(@RequestParam Long categoryId) {
        log.info("existsByCategoryId - " + categoryId);
        return eventService.existsByCategoryId(categoryId);
    }

    @GetMapping("/events/{eventId}")
    EventFullDto getEventById(@PathVariable("eventId") Long eventId) {
        log.info("getEventDtoById - " + eventId);
        return eventService.getEventDtoById(eventId);
    }

    @PostMapping("/{id}/confirmed")
    public void updateConfirmedRequests(@PathVariable("id") Long eventId, @RequestParam int delta) {
        log.info("updateConfirmedRequests - " + eventId);
        eventService.updateConfirmedRequests(eventId, delta);
    }

    @GetMapping("/{id}/short")
    public ResponseEntity<EventShortDto> getEventShort(@PathVariable Long id) {
        EventShortDto dto = eventService.getEventShort(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/short")
    public ResponseEntity<List<EventShortDto>> getEventsShortDto(@RequestParam List<Long> ids) {
        List<EventShortDto> events = eventService.getEventsShortDto(ids);
        return ResponseEntity.ok(events);
    }

    @GetMapping
    public List<EventShortDto> searchPublicEvents(
            @RequestParam(defaultValue = DEFAULT_TEXT) String text,
            @RequestParam(name = "categories", required = false) List<Long> categoriesIds,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = DATE_TIME_PATTERN) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") boolean onlyAvailable,
            @RequestParam(name = "sort", required = false) EventSort eventSort,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_START) @PositiveOrZero int from,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Positive int size,
            HttpServletRequest request) {
        log.info("Запрос на получение опубликованных событий: text='{}', " +
                        "categoriesIds={}, paid={}, start={}, end={}, onlyAvailable={}, eventSort={}",
                text, categoriesIds, paid, rangeStart, rangeEnd, onlyAvailable, eventSort);
        validateTimeRange(rangeStart, rangeEnd);
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);

        PageRequest pageRequest = createPageRequest(from, size, eventSort);
        SearchPublicEventsParamDto searchPublicEventsParamDto =
                SearchPublicEventsParamDto.builder().text(text)
                        .categoriesIds(categoriesIds)
                        .paid(paid)
                        .rangeStart(rangeStart)
                        .rangeEnd(rangeEnd)
                        .onlyAvailable(onlyAvailable)
                        .pageRequest(pageRequest)
                        .build();

        List<EventShortDto> eventShortDtos = eventService.searchPublicEvents(searchPublicEventsParamDto);
        List<Long> eventShortDtoIds = eventShortDtos.stream().map(EventShortDto::getId).toList();

        return eventShortDtos;
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(
            @PathVariable @Positive Long eventId,
            HttpServletRequest request,
            @RequestHeader("X-EWM-USER-ID") long userId) {
        log.info("Запрос на получение опубликованого события с id {}", eventId);
        EventFullDto eventFullDto = eventService.getPublicEvent(eventId, request, userId);

        return eventFullDto;
    }

    private PageRequest createPageRequest(int from, int size, EventSort sort) {
        int page = from / size;
        Sort sorting = (sort == EventSort.VIEWS)
                ? Sort.by(Sort.Direction.DESC, "views")
                : Sort.by(Sort.Direction.ASC, "eventDate");

        return PageRequest.of(page, size, sorting);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Время начала должно быть до окончания");
        }
    }

    public List<String> buildUrisFromPathAndIds(String uriPath, List<Long> ids) {
        return ids.stream()
                .map(id -> uriPath + "/" + id)
                .collect(Collectors.toList());
    }
}