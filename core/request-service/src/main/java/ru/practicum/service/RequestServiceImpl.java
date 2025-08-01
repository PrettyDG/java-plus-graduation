package ru.practicum.service;


import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.event.EventFullDto;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatusEntity;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.RequestStatusRepository;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.UserDto;
import ru.practicum.validator.RequestValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestStatusRepository requestStatusRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestValidator requestValidator;

    @Override
    public List<ParticipationRequestDto> findByEventId(Long eventId) {
        log.debug("Запрос на получение заявок по событию с ID: {}", eventId);
        return requestRepository.findByEventId(eventId).stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> findByIds(List<Long> ids) {
        log.debug("Запрос на получение заявок по списку ID: {}", ids);
        return requestRepository.findAllById(ids).stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public List<ParticipationRequestDto> saveAll(List<ParticipationRequestDto> requestDtos) {
        log.debug("Запрос на массовое сохранение заявок");

        List<Request> requests = requestDtos.stream()
                .map(RequestMapper::toEntity)
                .collect(Collectors.toList());

        List<Request> saved = requestRepository.saveAll(requests);

        return saved.stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.debug("Запрос на получение всех заявок участия пользователя с ID: {}", userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipationRequestDto createParticipationRequest(Long userId, Long eventId) {
        final UserDto user = getUserById(userId);
        final EventFullDto event = getEventById(eventId);

        requestValidator.validateRequestCreation(user, event);

        final Request request = buildNewRequest(user, event);
        determineInitialStatus(event, request);

        final Request savedRequest = requestRepository.save(request);
        updateEventStatistics(event, request.getStatus().getName());

        log.info("Заявка на участие сохранена со статусом с ID: {} и статусом: {}",
                savedRequest.getId(), savedRequest.getStatus());
        return RequestMapper.toRequestDto(savedRequest);
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        final UserDto user = getUserById(userId);
        final Request request = getRequestById(requestId);

        requestValidator.validateRequestOwnership(user, request);
        updateRequestStatus(request, RequestStatus.CANCELED);

        if (request.getStatus().getName() == RequestStatus.CONFIRMED) {
            adjustEventConfirmedRequests(request.getEventId(), -1);
        }

        log.info("Заявка на участие с id = {} отменена пользователем ID: {}", requestId, userId);
        return RequestMapper.toRequestDto(request);
    }

    private UserDto getUserById(Long userId) {
        try {
            return userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Не найден пользователь с ID: " + userId);
        }
    }

    private EventFullDto getEventById(Long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Не найдено событие с ID: " + eventId);
        }
    }

    private Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Не найдена заявка с ID: " + requestId));
    }

    private RequestStatusEntity getRequestStatusEntityByRequestStatus(RequestStatus newStatus) {
        return requestStatusRepository.findByName(newStatus)
                .orElseThrow(() -> new NotFoundException("Не найден статус: " + newStatus.name()));
    }

    private Request buildNewRequest(UserDto user, EventFullDto event) {
        RequestStatusEntity requestStatusEntity = getRequestStatusEntityByRequestStatus(RequestStatus.PENDING);
        return Request.builder()
                .requesterId(user.getId())
                .eventId(event.getId())
                .created(LocalDateTime.now())
                .status(requestStatusEntity)
                .build();
    }

    private void determineInitialStatus(EventFullDto event, Request request) {
        if (shouldAutoConfirm(event)) {
            request.setStatus(getRequestStatusEntityByRequestStatus(RequestStatus.CONFIRMED));
        } else if (isEventFull(event)) {
            request.setStatus(getRequestStatusEntityByRequestStatus(RequestStatus.REJECTED));
        }
    }

    private boolean shouldAutoConfirm(EventFullDto event) {
        return event.getParticipantLimit() == 0 ||
                (!event.getRequestModeration() && hasAvailableSlots(event));
    }

    private boolean isEventFull(EventFullDto event) {
        return event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit();
    }

    private boolean hasAvailableSlots(EventFullDto event) {
        return event.getConfirmedRequests() < event.getParticipantLimit();
    }

    private void updateEventStatistics(EventFullDto event, RequestStatus status) {
        if (status == RequestStatus.CONFIRMED) {
            adjustEventConfirmedRequests(event.getId(), 1);
        }
    }

    //TODO
    private void adjustEventConfirmedRequests(Long eventId, int delta) {
        eventClient.updateConfirmedRequests(eventId, delta);
    }

    private void updateRequestStatus(Request request, RequestStatus newStatus) {
        String currentStatusName = request.getStatus().getName().name();
        if (currentStatusName.equals(newStatus.name())) {
            throw new ValidationException("Статус уже установлен: " + newStatus);
        }
        request.setStatus(getRequestStatusEntityByRequestStatus(newStatus));
    }
}

