package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
public class MessageProducerImpl implements MessageProducer {
    private final KafkaProperties properties;
    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Override
    public void sendUserAction(UserActionAvro userActionAvro) {
        kafkaTemplate.send(properties.getUserActionsTopic(), userActionAvro);
    }
}