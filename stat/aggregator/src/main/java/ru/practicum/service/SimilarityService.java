package ru.practicum.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.KafkaProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SimilarityService {

    private final Map<Long, Map<Long, Double>> weights = new HashMap<>();

    private final Map<Long, Double> eventWeightsSum = new HashMap<>();

    private final MinWeightsMatrix minWeightsMatrix = new MinWeightsMatrix();

    private final KafkaTemplate<String, EventSimilarityAro> kafkaTemplate;
    private final KafkaProperties props;

    public SimilarityService(KafkaTemplate<String, EventSimilarityAro> kafkaTemplate,
                             KafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    public void processUserAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();

        double actionWeight = 0;
        ActionTypeAvro actionTypeAvro = action.getActionType();
        if (actionTypeAvro == ActionTypeAvro.VIEW) {
            actionWeight = 0.4;
        } else if (actionTypeAvro == ActionTypeAvro.REGISTER) {
            actionWeight = 0.8;
        } else if (actionTypeAvro == ActionTypeAvro.LIKE) {
            actionWeight = 1;
        }

        Instant timestamp = action.getTimestamp();

        Map<Long, Double> userMap = weights.computeIfAbsent(eventId, e -> new HashMap<>());
        Double oldWeight = userMap.getOrDefault(userId, 0.0);

        if (actionWeight <= oldWeight) {
            log.debug("Обновление не требуется: userId={}, eventId={}, weight={} <= oldWeight={}",
                    userId, eventId, actionWeight, oldWeight);
            return;
        }

        userMap.put(userId, actionWeight);

        Double oldSum = eventWeightsSum.getOrDefault(eventId, 0.0);
        Double diff = actionWeight - oldWeight;
        Double updatedSum = oldSum + diff;
        eventWeightsSum.put(eventId, updatedSum);

        weights.keySet()
                .stream()
                .filter(otherEvent -> otherEvent.equals(eventId))
                .forEach(otherEvent -> updatePairSimilarity(eventId, otherEvent, timestamp));
    }

    private void updatePairSimilarity(long eventA, long eventB, Instant timestamp) {
        double sMin = calcSMin(eventA, eventB);
        minWeightsMatrix.put(eventA, eventB, sMin);

        double sA = eventWeightsSum.getOrDefault(eventA, 0.0);
        double sB = eventWeightsSum.getOrDefault(eventB, 0.0);
        if (sA == 0 || sB == 0) {

            log.debug("Обнаружена нулевая сумма (sA={}, sB={}), пропускающая сходство для событий {} и {}",
                    sA, sB, eventA, eventB);
            return;
        }

        float similarity = (float) (sMin / (sA * sB));

        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        EventSimilarityAro similarityMsg = EventSimilarityAro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();

        kafkaTemplate.send(props.getProducer().getTopic(), similarityMsg);

        log.debug("Обновлено сходство для (A={}, B={}) => {}", first, second, similarity);
    }

    private double calcSMin(long eventA, long eventB) {
        Map<Long, Double> userMapA = weights.getOrDefault(eventA, Map.of());
        Map<Long, Double> userMapB = weights.getOrDefault(eventB, Map.of());

        return userMapA.entrySet().stream()
                .filter(e -> userMapB.get(e.getKey()) != null)
                .mapToDouble(e -> Math.min(e.getValue(), userMapB.get(e.getKey())))
                .sum();
    }

    private int convertActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case REGISTER -> 2;
            case LIKE -> 3;
            default -> 1;
        };
    }
}