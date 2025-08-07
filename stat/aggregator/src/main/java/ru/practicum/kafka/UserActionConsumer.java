package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AggregatorService;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "stats.user-actions.v1", containerFactory = "kafkaListenerFactory")
    public void consume(UserActionAvro action) {
        log.info("User action received - : {}", action);
        aggregatorService.handle(action);
    }
}
