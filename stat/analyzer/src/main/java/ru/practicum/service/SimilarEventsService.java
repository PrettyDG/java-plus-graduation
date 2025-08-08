package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAro;
import ru.practicum.model.EventSimilarityEntity;
import ru.practicum.repository.EventSimilarityRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarEventsService {

    private final EventSimilarityRepository eventSimilarityRepository;

    public void updateEventSimilarity(EventSimilarityAro eventSimilarityAvro) {
        long eventA = eventSimilarityAvro.getEventA();
        long eventB = eventSimilarityAvro.getEventB();
        double score = eventSimilarityAvro.getScore();
        Instant ts = eventSimilarityAvro.getTimestamp();

        EventSimilarityEntity existing = findPair(eventA, eventB);
        if (existing == null) {
            existing = new EventSimilarityEntity();
            existing.setEventA(eventA);
            existing.setEventB(eventB);
            existing.setScore(score);
            existing.setTimestamp(ts);
            eventSimilarityRepository.save(existing);
        } else {
            existing.setScore(score);
            existing.setTimestamp(ts);
            eventSimilarityRepository.save(existing);
        }
    }

    private EventSimilarityEntity findPair(long eventA, long eventB) {
        return eventSimilarityRepository.findByEventAOrEventB(eventA, eventB)
                .stream()
                .filter(e -> (e.getEventA().equals(eventA) && e.getEventB().equals(eventB))
                        || (e.getEventA().equals(eventB) && e.getEventB().equals(eventA)))
                .findFirst()
                .orElse(null);
    }
}