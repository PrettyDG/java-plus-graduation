package ru.practicum.service;


import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.UserClient;
import ru.practicum.clients.EventClient;
import ru.practicum.event.EventFullDto;
import ru.practicum.exceptions.ConflictException;
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
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> findByEventId(Long eventId) {
        log.debug("Запрос на получение заявок по событию с ID: {}", eventId);
        List<ParticipationRequestDto> result = requestRepository.findByEventId(eventId).stream()
                .map(request -> {
                    UserDto userDto = userClient.getUser(request.getRequesterId());
                    return RequestMapper.toRequestDto(request, userDto);
                })
                .collect(Collectors.toList());
        log.info("Найдено {} заявок для события с ID: {}", result.size(), eventId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> findByIds(List<Long> ids) {
        log.debug("Запрос на получение заявок по списку ID: {}", ids);
        List<ParticipationRequestDto> result = requestRepository.findAllById(ids).stream()
                .map(request -> {
                    UserDto userDto = userClient.getUser(request.getRequesterId());
                    return RequestMapper.toRequestDto(request, userDto);
                })
                .collect(Collectors.toList());
        log.info("Найдено {} заявок по списку ID, {}", result.size(), result);
        return result;
    }

    @Override
    public List<ParticipationRequestDto> saveAll(List<ParticipationRequestDto> requestDtos) {
        log.debug("Запрос на массовое сохранение заявок: {}", requestDtos.size());


        List<Request> requests = requestDtos.stream()
                .map(dto -> {
                    RequestStatusEntity statusEntity = requestStatusRepository.findByName(dto.getStatus())
                            .orElseThrow(() -> new RuntimeException("Unknown status: " + dto.getStatus()));
                    return RequestMapper.toEntity(dto, statusEntity);
                })
                .collect(Collectors.toList());

        requestRepository.saveAll(requests);
        log.info("requests - " + requests);

        List<ParticipationRequestDto> result = requests.stream()
                .map(request -> {
                    UserDto userDto = userClient.getUser(request.getRequesterId());
                    return RequestMapper.toRequestDto(request, userDto);
                })
                .collect(Collectors.toList());
        log.info("result - " + result);

        log.info("Массовое сохранение заявок завершено, сохранено: {}", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.debug("Запрос на получение всех заявок участия пользователя с ID: {}", userId);
        List<ParticipationRequestDto> result = requestRepository.findByRequesterId(userId).stream()
                .map(request -> {
                    UserDto userDto = userClient.getUser(request.getRequesterId());
                    return RequestMapper.toRequestDto(request, userDto);
                })
                .collect(Collectors.toList());
        log.info("Найдено {} заявок пользователя с ID: {}", result.size(), userId);
        return result;
    }

    @Override
    public ParticipationRequestDto createParticipationRequest(Long userId, Long eventId) {
        log.info("Создание заявки на участие пользователя с ID: {} для события с ID: {}", userId, eventId);
        final UserDto user = getUserById(userId);
        log.info("user - " + user);
        final EventFullDto event = getEventById(userId, eventId);
        log.info("event - " + event);
        requestValidator.validateRequestCreation(user, event);

        final Request request = buildNewRequest(user, event);
        determineInitialStatus(event, request);

        final Request savedRequest = requestRepository.save(request);
        updateEventStatistics(event, request.getStatus().getName());

        log.info("Заявка на участие создана с ID: {} и статусом: {}", savedRequest.getId(), savedRequest.getStatus());
        return RequestMapper.toRequestDto(savedRequest, user);
    }

    @Override
    public ParticipationRequestDto cancelParticipationRequest(Long userId, Long requestId) {
        log.debug("Отмена заявки на участие с ID: {} пользователем с ID: {}", requestId, userId);
        final UserDto user = getUserById(userId);
        final Request request = getRequestById(requestId);

        requestValidator.validateRequestOwnership(user, request);
        updateRequestStatus(request, RequestStatus.CANCELED);

        if (request.getStatus().getName() == RequestStatus.CONFIRMED) {
            adjustEventConfirmedRequests(request.getEventId(), -1);
        }

        log.info("Заявка на участие с ID: {} отменена пользователем с ID: {}", requestId, userId);
        return RequestMapper.toRequestDto(request, user);
    }

    @Transactional(readOnly = true)
    @Override
    public RequestStatus getStatusByName(String name) {
        RequestStatusEntity requestStatusEntity = requestStatusRepository.findByName(RequestStatus.valueOf(name))
                .orElseThrow(() -> {
                    log.error("Статус не найден: {}", name);
                    return new NotFoundException("Не найден статус: " + name);
                });

        return requestStatusEntity.getName();
    }

    @Transactional(readOnly = true)
    private UserDto getUserById(Long userId) {
        log.debug("Получение пользователя по ID: {}", userId);
        try {
            UserDto user = userClient.getUser(userId);
            log.info("Пользователь найден с ID: {}", userId);
            return user;
        } catch (FeignException.NotFound e) {
            log.error("Пользователь с ID: {} не найден", userId);
            throw new NotFoundException("Не найден пользователь с ID: " + userId);
        }
    }

    @Transactional(readOnly = true)
    private EventFullDto getEventById(Long userId, Long eventId) {
        log.info("Получение события по ID: {}", eventId);
        try {
            EventFullDto event = eventClient.getEventById(eventId);
            log.info("Событие найдено с ID: {}", eventId);
            return event;
        } catch (FeignException.NotFound e) {
            log.error("Событие с ID: {} не найдено", eventId);
            throw new ConflictException("Не найдено событие с ID: " + eventId);
        }
    }

    @Transactional(readOnly = true)
    private Request getRequestById(Long requestId) {
        log.debug("Получение заявки по ID: {}", requestId);
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Заявка с ID: {} не найдена", requestId);
                    return new NotFoundException("Не найдена заявка с ID: " + requestId);
                });
        log.info("request - " + request);
        return request;
    }

    @Transactional(readOnly = true)
    private RequestStatusEntity getRequestStatusEntityByRequestStatus(RequestStatus newStatus) {
        log.debug("Получение сущности статуса по статусу: {}", newStatus);
        return requestStatusRepository.findByName(newStatus)
                .orElseThrow(() -> {
                    log.error("Статус не найден: {}", newStatus.name());
                    return new NotFoundException("Не найден статус: " + newStatus.name());
                });
    }

    @Transactional(readOnly = true)
    private Request buildNewRequest(UserDto user, EventFullDto event) {
        log.debug("Построение новой заявки для пользователя ID: {} и события ID: {}", user.getId(), event.getId());
        RequestStatusEntity requestStatusEntity = getRequestStatusEntityByRequestStatus(RequestStatus.PENDING);
        return Request.builder()
                .requesterId(user.getId())
                .eventId(event.getId())
                .created(LocalDateTime.now())
                .status(requestStatusEntity)
                .build();
    }

    private void determineInitialStatus(EventFullDto event, Request request) {
        log.debug("Определение начального статуса для заявки на событие с ID: {}", event.getId());
        if (shouldAutoConfirm(event)) {
            request.setStatus(getRequestStatusEntityByRequestStatus(RequestStatus.CONFIRMED));
            log.info("Заявка будет автоматически подтверждена");
        } else if (isEventFull(event)) {
            request.setStatus(getRequestStatusEntityByRequestStatus(RequestStatus.REJECTED));
            log.info("Событие заполнено, заявка будет отклонена");
        } else {
            log.info("Заявка останется в статусе ожидания подтверждения");
        }
    }

    private boolean shouldAutoConfirm(EventFullDto event) {
        boolean result = event.getParticipantLimit() == 0 ||
                (!event.getRequestModeration() && hasAvailableSlots(event));
        log.debug("Проверка авто-подтверждения заявки: {}", result);
        return result;
    }

    private boolean isEventFull(EventFullDto event) {
        boolean result = event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit();
        log.debug("Проверка заполненности события: {}", result);
        return result;
    }

    private boolean hasAvailableSlots(EventFullDto event) {
        boolean result = event.getConfirmedRequests() < event.getParticipantLimit();
        log.debug("Проверка доступных слотов в событии: {}", result);
        return result;
    }

    private void updateEventStatistics(EventFullDto event, RequestStatus status) {
        log.debug("Обновление статистики события с ID: {}, статус заявки: {}", event.getId(), status);
        if (status == RequestStatus.CONFIRMED) {
            adjustEventConfirmedRequests(event.getId(), 1);
            log.info("Статистика события обновлена: увеличено количество подтвержденных заявок");
        }
    }

    private void adjustEventConfirmedRequests(Long eventId, int delta) {
        log.debug("Изменение количества подтвержденных заявок события с ID: {} на {}", eventId, delta);
        eventClient.updateConfirmedRequests(eventId, delta);
    }

    private void updateRequestStatus(Request request, RequestStatus newStatus) {
        log.debug("Обновление статуса заявки с ID: {} с {} на {}", request.getId(), request.getStatus().getName(), newStatus);
        String currentStatusName = request.getStatus().getName().name();
        if (currentStatusName.equals(newStatus.name())) {
            log.error("Попытка установить уже существующий статус: {}", newStatus);
            throw new ValidationException("Статус уже установлен: " + newStatus);
        }
        request.setStatus(getRequestStatusEntityByRequestStatus(newStatus));
        log.info("Статус заявки с ID: {} обновлен на {}", request.getId(), newStatus);
    }
}