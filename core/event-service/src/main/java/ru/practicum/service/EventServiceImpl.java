package ru.practicum.service;

import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.practicum.AnalyzerClient;
import ru.practicum.CollectorClient;
import ru.practicum.category.CategoryDto;
import ru.practicum.client.CategoryClient;
import ru.practicum.client.UserClient;
import ru.practicum.clients.RequestClient;
import ru.practicum.event.*;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Event;
import ru.practicum.model.Location;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.LocationRepository;
import ru.practicum.request.EventRequestStatusUpdateRequest;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.UserDto;
import ru.practicum.validator.EventValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventValidator eventValidator;
    private final LocationRepository locationRepository;
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final RequestClient requestClient;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;

    @Override
    public void addLike(long userId, Long eventId) {
        if (requestClient.isRequestExist(eventId, userId)) {
            collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
        } else {
            throw new ValidationException("Лайт не поставлен, пользователь ещё не подал заявку на участие");
        }
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        boolean isExist = eventRepository.existsByCategoryId(categoryId);
        log.info("isExist - " + isExist);
        return isExist;
    }

    @Override
    public EventFullDto getEventDtoById(Long eventId) {
        Event event = eventRepository.findById(eventId).get();
        log.info("getEventDtoById, Event - " + event + ", UserForEvent - " + event.getInitiatorId());
        UserDto userDto = userClient.getUser(event.getInitiatorId());
        CategoryDto categoryDto = categoryClient.getCategoryById(event.getCategoryId());
        double rating = analyzerClient.getInteractionsCount(List.of(eventId)).get(eventId);
        EventFullDto eventFullDto = EventMapper.toFullDto(event, categoryDto, userDto, rating);
        log.info("EventFullDto - " + eventFullDto);
        return eventFullDto;
    }

    @Override
    public void updateConfirmedRequests(Long eventId, int delta) {
        Event event = eventRepository.findById(eventId).get();
        log.info("Event - " + event);
        event.setConfirmedRequests(event.getConfirmedRequests() + delta);
        eventRepository.save(event);
        log.info("Сохранён event после updateConfirmedRequests - " + event);
    }

    @Transactional(readOnly = true)
    @Override
    public EventShortDto getEventShort(Long id) {
        Event event = eventRepository.findById(id).get();
        log.info("getEventShort, Event - " + event + ", UserForEvent - " + event.getInitiatorId());
        UserDto userDto = userClient.getUser(event.getInitiatorId());
        CategoryDto categoryDto = categoryClient.getCategoryById(event.getCategoryId());
        double rating = analyzerClient.getInteractionsCount(List.of(id)).get(id);
        EventShortDto eventShortDto = EventMapper.toShortDto(event, categoryDto, userDto, rating);
        log.info("EventShortDto - " + eventShortDto);
        return eventShortDto;
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventShortDto> getEventsShortDto(List<Long> ids) {
        log.info("getEventsShortDto - " + ids);
        return ids.stream()
                .map(this::getEventShort)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        log.info("getUserEvents - " + userId + ", - " + pageable);
        eventValidator.validateUserExists(userId);

        return eventRepository.findByInitiatorId(userId, pageable)
                .stream()
                .map(event -> {
                    CategoryDto categoryDto = getCategoryById(event.getCategoryId());
                    UserDto userDto = getUserById(event.getInitiatorId());
                    double rating = analyzerClient.getInteractionsCount(List.of(event.getId())).get(event.getId());
                    return EventMapper.toShortDto(event, categoryDto, userDto, rating);
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto createEvent(Long userId, NewEventDto request) {
        log.info("createEvent - " + userId + ", - " + request);
        UserDto userDto = getUserById(userId);
        CategoryDto category = getCategoryById(request.getCategory());
        Location location = resolveLocation(LocationMapper.toLocation(request.getLocation()));

        Event event = EventMapper.toEvent(request, userId, category);
        event.setLocation(location);
        event.setState(EventState.PENDING);

        Event savedEvent = eventRepository.save(event);
        log.info("Событие успешно добавлено под id {} со статусом {} и ожидается подтверждение",
                userId, event.getState());
        double rating = analyzerClient.getInteractionsCount(List.of(savedEvent.getId())).get(savedEvent.getId());
        EventFullDto eventFullDto = EventMapper.toFullDto(savedEvent, category, userDto, rating);
        log.info("Возвращаем eventFullDto - " + eventFullDto + "userId - " + eventFullDto.getInitiator().getId());
        return eventFullDto;
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto getUserEventById(Long userId,
                                         Long eventId) {
        log.info("getUserEventById, userId - " + userId + ", eventId - " + eventId);
        Event event = getEventById(eventId);
        log.info("event - " + event);
        UserDto userDto = getUserById(userId);
        log.info("userdto - " + userDto);
        CategoryDto categoryDto = getCategoryById(event.getCategoryId());
        log.info("categoryDto - " + categoryDto);
        eventValidator.validateEventOwnership(event, userId);
        double rating = analyzerClient.getInteractionsCount(List.of(eventId)).get(eventId);
        EventFullDto fullDto = EventMapper.toFullDto(event, categoryDto, userDto, rating);
        log.info("EventFullDto - " + fullDto);
        return fullDto;
    }

    @Override
    public EventFullDto updateUserEvent(Long userId,
                                        Long eventId,
                                        UpdateEventUserRequest updateDto) {
        log.info("updateUserEvent - " + userId + ", - " + eventId + ", -" + updateDto);
        Event event = getEventById(eventId);

        eventValidator.validateUserUpdate(event, userId, updateDto);
        applyUserUpdates(event, updateDto);

        Event updatedEvent = eventRepository.save(event);
        UserDto userDto = getUserById(userId);
        CategoryDto categoryDto = getCategoryById(event.getCategoryId());
        log.info("Событие успешно обновлено под id {} и дожидается подтверждения", eventId);
        double rating = analyzerClient.getInteractionsCount(List.of(eventId)).get(eventId);
        return EventMapper.toFullDto(updatedEvent, categoryDto, userDto, rating);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("getEventRequests - " + userId + ", - " + eventId);
        Event event = getEventById(eventId);
        eventValidator.validateEventOwnership(event, userId);
        List<ParticipationRequestDto> participationRequestDtos = requestClient.getRequestsByEventId(eventId);
        log.info("participitionRequestDto - " + participationRequestDtos);
        return participationRequestDtos;
    }

    public Map<String, List<ParticipationRequestDto>> approveRequests(Long userId,
                                                                      Long eventId,
                                                                      EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        log.info("approveRequests - " + userId + ", - " + eventId + ", - " + eventRequestStatusUpdateRequest);
        Event event = getEventById(eventId);
        eventValidator.validateInitiator(event, userId);

        List<ParticipationRequestDto> requests = getAndValidateRequests(eventId, eventRequestStatusUpdateRequest.getRequestIds());
        RequestStatus status = eventRequestStatusUpdateRequest.getStatus();

        if (status == RequestStatus.CONFIRMED) {
            eventValidator.validateParticipantLimit(event);
        }

        return processStatusSpecificLogic(event, requests, status);
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventFullDto> searchEventsByAdmin(SearchAdminEventsParamDto searchParams) {
        log.info("searchEventsByAdmin - " + searchParams);

        return eventRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();

                    // Фильтр по пользователям
                    if (searchParams.getUsers() != null && !searchParams.getUsers().isEmpty()) {
                        predicates.add(root.get("initiatorId").in(searchParams.getUsers()));
                    }

                    // Фильтр по состояниям
                    if (searchParams.getEventStates() != null && !searchParams.getEventStates().isEmpty()) {
                        predicates.add(root.get("state").in(searchParams.getEventStates()));
                    }

                    // Фильтр по категориям
                    if (searchParams.getCategoriesIds() != null && !searchParams.getCategoriesIds().isEmpty()) {
                        predicates.add(root.get("category").get("id").in(searchParams.getCategoriesIds()));
                    }

                    // Фильтр по датам
                    predicates.add(cb.between(root.get("eventDate"), searchParams.getRangeStart(),
                            searchParams.getRangeEnd()));

                    return cb.and(predicates.toArray(new Predicate[0]));
                }, searchParams.getPageRequest()).stream()
                .map(event -> {
                    CategoryDto categoryDto = getCategoryById(event.getCategoryId());
                    UserDto userDto = getUserById(event.getInitiatorId());
                    double rating = analyzerClient.getInteractionsCount(List.of(event.getId())).get(event.getId());
                    return EventMapper.toFullDto(event, categoryDto, userDto, rating);
                })
                .collect(Collectors.toList());
    }


    @Override
    public EventFullDto updateEventByAdmin(Long eventId,
                                           UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("updateEventByAdmin - " + eventId + ", - " + updateEventAdminRequest);

        Event oldEvent = getEventById(eventId);
        eventValidator.validateAdminPublishedEventDate(updateEventAdminRequest.getEventDate(), oldEvent);
        eventValidator.validateAdminEventDate(oldEvent);
        eventValidator.validateAdminEventUpdateState(oldEvent.getState());
        applyAdminUpdates(oldEvent, updateEventAdminRequest);
        Event event = eventRepository.save(oldEvent);
        log.info("Событие успешно обновлено администратором, - " + event);
        CategoryDto categoryDto = getCategoryById(event.getCategoryId());
        UserDto userDto = getUserById(event.getInitiatorId());
        double rating = analyzerClient.getInteractionsCount(List.of(eventId)).get(eventId);
        EventFullDto eventFullDto = EventMapper.toFullDto(event, categoryDto, userDto, rating);
        log.info("Возвращаем eventFullDto - " + eventFullDto + "userId - " + eventFullDto.getInitiator().getId());

        return eventFullDto;
    }

    private void handleStateUpdateEventAdminRequest(StateAction action, Event event) {
        log.info("handleStateUpdateEventAdminRequest - " + action + ", - " + event);
        if (event.getState() != EventState.PENDING) {
            throw new ConflictException("Изменение статуса возможно только для событий в состоянии PENDING");
        }

        switch (action) {
            case PUBLISH_EVENT -> {
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            }
            case REJECT_EVENT -> {
                event.setState(EventState.CANCELED);
                event.setPublishedOn(null);
            }
            default -> throw new UnsupportedOperationException("Неподдерживаемая операция: " + action);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventShortDto> searchPublicEvents(SearchPublicEventsParamDto searchParams) {
        log.info("searchPublicEvents - " + searchParams);

        Specification<Event> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Базовые условия
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), searchParams.getRangeStart()));
            predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), searchParams.getRangeEnd()));
            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            // Фильтр по тексту
            if (StringUtils.hasText(searchParams.getText())) {
                String searchTerm = "%" + searchParams.getText().toLowerCase() + "%";
                Predicate annotationLike = cb.like(cb.lower(root.get("annotation")), searchTerm);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), searchTerm);
                predicates.add(cb.or(annotationLike, descriptionLike));
            }

            // Фильтр по категориям
            if (searchParams.getCategoriesIds() != null && !searchParams.getCategoriesIds().isEmpty()) {
                predicates.add(root.get("categoryId").in(searchParams.getCategoriesIds()));
            }

            // Фильтр по paid
            if (searchParams.getPaid() != null) {
                predicates.add(cb.equal(root.get("paid"), searchParams.getPaid()));
            }

            // Фильтр по доступности
            if (searchParams.isOnlyAvailable()) {
                predicates.add(cb.gt(root.get("participantLimit"), root.get("confirmedRequests")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Event> eventsPage = eventRepository.findAll(
                specification,
                PageRequest.of(searchParams.getPageRequest().getPageNumber(),
                        searchParams.getPageRequest().getPageSize(),
                        searchParams.getPageRequest().getSort())
        );

        List<Event> events = eventsPage.getContent();
        if (events.isEmpty()) {
            return new ArrayList<>();
        }
        return paginateAndMap(events, searchParams.getPageRequest());
    }

    @Transactional(readOnly = true)
    @Override
    public EventFullDto getPublicEvent(Long eventId,
                                       HttpServletRequest request,
                                       long userId) {
        log.info("getPublicEvent. eventId - " + eventId + ",request - " + request);
        Event event = getEventById(eventId);
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("У события должен быть статус <PUBLISHED>");
        }
        UserDto userDto = getUserById(event.getInitiatorId());
        CategoryDto categoryDto = getCategoryById(event.getCategoryId());
        collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
        double rating = analyzerClient.getInteractionsCount(List.of(eventId)).get(eventId);
        EventFullDto eventFullDto = EventMapper.toFullDto(event, categoryDto, userDto, rating);
        log.info("Возвращаем eventFullDto - " + eventFullDto + "userId - " + eventFullDto.getInitiator().getId());
        return eventFullDto;
    }

    private List<EventShortDto> paginateAndMap(List<Event> events, PageRequest pageRequest) {
        log.info("paginateAndMap - " + events + ", - " + pageRequest);
        List<Event> paginatedEvents = events.stream()
                .skip(pageRequest.getOffset())
                .toList();

        return paginatedEvents.stream()
                .map(event -> {
                    CategoryDto categoryDto = getCategoryById(event.getCategoryId());
                    UserDto userDto = getUserById(event.getInitiatorId());
                    double rating = analyzerClient.getInteractionsCount(List.of(event.getId())).get(event.getId());
                    return EventMapper.toShortDto(event, categoryDto, userDto, rating);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    private UserDto getUserById(Long userId) {
        log.info("getUserById - " + userId);
        try {
            return userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }

    @Transactional(readOnly = true)
    private Event getEventById(Long eventId) {
        log.info("getEventById, eventId - " + eventId);
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Не найдено событие с ID: " + eventId));
    }

    @Transactional(readOnly = true)
    private CategoryDto getCategoryById(Long categoryId) {
        log.info("getCategoryById - " + categoryId);
        try {
            return categoryClient.getCategoryById(categoryId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Указана не правильная ID категории: " + categoryId);
        }
    }

    private Location resolveLocation(Location requestLocation) {
        log.info("resolveLocation - " + requestLocation);
        Location mayBeExistingLocation = null;
        if (requestLocation.getId() == null) {
            mayBeExistingLocation = locationRepository
                    .findByLatAndLon(requestLocation.getLat(), requestLocation.getLon())
                    .orElseGet(() -> locationRepository.save(requestLocation));
        }
        return mayBeExistingLocation;
    }

    private void applyAdminUpdates(Event event, UpdateEventAdminRequest update) {
        log.info("applyAdminUpdates - " + event + ", - " + update);

        Optional.ofNullable(update.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(update.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(update.getEventDate()).ifPresent(event::setEventDate);
        Optional.ofNullable(update.getLocation())
                .map(LocationMapper::toLocation)
                .ifPresent(event::setLocation);
        Optional.ofNullable(update.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(update.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(update.getRequestModeration()).ifPresent(event::setRequestModeration);
        Optional.ofNullable(update.getTitle()).ifPresent(event::setTitle);

        Optional.ofNullable(update.getCategory())
                .map(categoryId -> {
                    CategoryDto categoryDto = categoryClient.getCategoryById(categoryId);
                    if (categoryDto == null) {
                        throw new ValidationException("Не найдена категория с ID: " + categoryId);
                    }
                    return categoryDto.getId();
                })
                .ifPresent(event::setCategoryId);

        Optional.ofNullable(update.getStateAction())
                .ifPresent(action -> handleStateUpdateEventAdminRequest(action, event));
    }

    private void applyUserUpdates(Event event, UpdateEventUserRequest update) {
        log.info("applyUserUpdates - " + event + ", - " + update);

        Optional.ofNullable(update.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(update.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(update.getEventDate()).ifPresent(event::setEventDate);
        Optional.ofNullable(update.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(update.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(update.getRequestModeration()).ifPresent(event::setRequestModeration);
        Optional.ofNullable(update.getTitle()).ifPresent(event::setTitle);

        Optional.ofNullable(update.getCategory())
                .ifPresent(event::setCategoryId);

        Optional.ofNullable(update.getLocation())
                .map(LocationMapper::toLocation)
                .map(this::resolveLocation)
                .ifPresent(event::setLocation);

        updateState(update.getStateAction(), event);
    }

    private void updateState(StateAction stateAction, Event event) {
        log.info("updateState - " + stateAction + ", - " + event);
        if (stateAction == null) return;
        switch (stateAction) {
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
        }
    }

    private Map<String, List<ParticipationRequestDto>> processStatusSpecificLogic(Event event,
                                                                                  List<ParticipationRequestDto> requests,
                                                                                  RequestStatus status) {
        log.info("processStatusSpecificLogic - " + event + ", - " + requests + ", " + status);
        if (status == RequestStatus.REJECTED) {
            return processRejection(requests);
        } else {
            return processConfirmation(event, requests);
        }
    }

    @Transactional(readOnly = true)
    private List<ParticipationRequestDto> getAndValidateRequests(Long eventId, List<Long> requestIds) {
        log.info("getAndValidateRequests - " + eventId + ", - " + requestIds);
        List<ParticipationRequestDto> requests = requestClient.getRequestsByIds(requestIds);
        eventValidator.validateRequestsBelongToEvent(requests, eventId);
        return requests;
    }

    private Map<String, List<ParticipationRequestDto>> processRejection(List<ParticipationRequestDto> requests) {
        log.info("processRejection - " + requests);
        eventValidator.validateNoConfirmedRequests(requests);
        updateRequestStatuses(requests, RequestStatus.REJECTED);
        List<ParticipationRequestDto> rejectedRequests = requestClient.saveAll(requests)
                .stream()
                .toList();

        return Map.of("rejectedRequests", rejectedRequests);
    }

    private void updateRequestStatuses(List<ParticipationRequestDto> requests, RequestStatus status) {
        log.info("updateRequestStatuses - " + requests + ", - " + status);
        RequestStatus requestStatus = requestClient.getStatusByName(status.name());
        log.info("requestStatus - " + requestStatus);

        requests.forEach(request -> request.setStatus(requestStatus));
        log.info("Requests - " + requests);
        requestClient.saveAll(requests);
    }

    private Map<String, List<ParticipationRequestDto>> processConfirmation(Event event, List<ParticipationRequestDto> requests) {
        log.info("processConfirmation - " + event + ", - " + requests);
        eventValidator.validateAllRequestsPending(requests);

        int availableSlots = event.getParticipantLimit() - event.getConfirmedRequests();
        List<ParticipationRequestDto> confirmed = requests.stream().limit(availableSlots).toList();
        List<ParticipationRequestDto> rejected = requests.stream().skip(availableSlots).toList();

        updateRequestStatuses(confirmed, RequestStatus.CONFIRMED);
        updateRequestStatuses(rejected, RequestStatus.REJECTED);

        requestClient.saveAll(requests);
        updateEventConfirmedRequests(event, confirmed.size());

        return Map.of(
                "confirmedRequests", mapToParticipationRequestDtoList(confirmed),
                "rejectedRequests", mapToParticipationRequestDtoList(rejected)
        );
    }

    private void updateEventConfirmedRequests(Event event, int newConfirmations) {
        log.info("updateEventConfirmedRequests - " + event + ", - " + newConfirmations);
        event.setConfirmedRequests(event.getConfirmedRequests() + newConfirmations);
        eventRepository.save(event);
    }

    private List<ParticipationRequestDto> mapToParticipationRequestDtoList(List<ParticipationRequestDto> requests) {
        log.info("mapToParticipationRequestDtoList - " + requests);
        return requests;
    }

}
