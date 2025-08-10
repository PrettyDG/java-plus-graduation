package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAro;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityProducer {

    private final KafkaTemplate<String, EventSimilarityAro> kafkaTemplate;
    private static final String TOPIC = "stats.events-similarity.v1";

    public void send(EventSimilarityAro similarity) {
        log.info("Publishing similarity: {}", similarity);
        kafkaTemplate.send(TOPIC, similarity);
    }
}
