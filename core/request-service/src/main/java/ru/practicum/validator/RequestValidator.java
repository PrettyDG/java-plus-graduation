package ru.practicum.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventState;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;
import ru.practicum.user.UserDto;


@Slf4j
@Component
public class RequestValidator {

    private final RequestRepository requestRepository;

    @Autowired
    public RequestValidator(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public void validateRequestCreation(UserDto user, EventFullDto event) {
        checkEventState(event);
        checkEventOwnership(user, event);
        checkDuplicateRequest(user.getId(), event.getId());
        checkEventCapacity(event);
    }

    private void checkEventState(EventFullDto event) {
        if (!event.getState().name().equals(EventState.PUBLISHED.name())) {
            throw new ConflictException("Нельзя подавать заявку на неопубликованное мероприятие");
        }
    }

    private void checkEventOwnership(UserDto user, EventFullDto event) {
        if (event.getInitiator().getId().equals(user.getId())) {
            throw new ConflictException("Пользователь не может подать заяку на участие в своем же мероприятии");
        }
    }

    private void checkDuplicateRequest(Long userId, Long eventId) {
        requestRepository.findByRequesterIdAndEventId(userId, eventId)
                .ifPresent(req -> {
                    throw new ConflictException("Пользователь: " +
                            userId + " уже подал заявку на участи в событии: " + eventId);
                });
    }

    public void validateRequestOwnership(UserDto user, Request request) {
        if (!request.getRequesterId().equals(user.getId())) {
            throw new ValidationException("Только пользователь подавший заявку может отменить ее. " +
                    "Пользователь ID: " + user.getId() +
                    "Заявка с ID: " + request.getId());
        }
    }

    private void checkEventCapacity(EventFullDto event) {
        if (event.getParticipantLimit() > 0 &&
                event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("Событие с ID: " + event.getId() + " нет свободных слотов");
        }
    }
}
