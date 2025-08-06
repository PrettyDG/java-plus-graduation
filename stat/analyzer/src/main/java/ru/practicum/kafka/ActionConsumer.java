package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.UserEventActionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionConsumer {

    private final UserEventActionService userActionService;

    @KafkaListener(
            topics = "${kafka.user-actions-consumer.topic}",
            containerFactory = "userActionsKafkaListenerFactory"
    )
    public void consumeUserActions(UserActionAvro actionAvro) {
        log.info("action consumed: {}", actionAvro);
        userActionService.updateUserAction(actionAvro);
    }
}