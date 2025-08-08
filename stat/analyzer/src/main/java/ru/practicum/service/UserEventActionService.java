package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.UserEventAction;
import ru.practicum.repository.UserActionRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventActionService {

    private final UserActionRepository userActionRepository;

    public void updateUserAction(UserActionAvro userActionAvro) {
        long userId = userActionAvro.getUserId();
        long eventId = userActionAvro.getEventId();

        double actionWeight = 0;
        ActionTypeAvro action = userActionAvro.getActionType();
        if (action == ActionTypeAvro.VIEW) {
            actionWeight = 0.4;
        } else if (action == ActionTypeAvro.REGISTER) {
            actionWeight = 0.8;
        } else if (action == ActionTypeAvro.LIKE) {
            actionWeight = 1;
        }

        Instant timestamp = userActionAvro.getTimestamp();

        UserEventAction userEventAction = userActionRepository.findByUserIdAndEventId(userId, eventId);
        if (userEventAction == null) {
            userEventAction = new UserEventAction();
            userEventAction.setUserId(userId);
            userEventAction.setEventId(eventId);
            userEventAction.setMaxWeight(actionWeight);
            userEventAction.setLastInteraction(timestamp);
            userActionRepository.save(userEventAction);
            return;
        }

        if (actionWeight > userEventAction.getMaxWeight()) {
            userEventAction.setMaxWeight(actionWeight);
        }

        if (timestamp.isAfter(userEventAction.getLastInteraction())) {
            userEventAction.setLastInteraction(timestamp);
        }

        userActionRepository.save(userEventAction);
    }
}