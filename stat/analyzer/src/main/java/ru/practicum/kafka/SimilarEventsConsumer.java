package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAro;
import ru.practicum.service.SimilarEventsService;

@Slf4j
@RequiredArgsConstructor
@Component
public class SimilarEventsConsumer {

    private final SimilarEventsService similarEventsService;

    @KafkaListener(
            topics = "${kafka.events-similarity-consumer.topic}",
            containerFactory = "eventSimilarityKafkaListenerFactory"
    )
    public void consumeEventSimilarity(EventSimilarityAro msg) {
        log.info("get consumeEventSimilarity: - " + msg);
        similarEventsService.updateEventSimilarity(msg);
    }
}
