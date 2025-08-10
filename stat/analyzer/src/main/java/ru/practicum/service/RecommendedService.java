package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;
import ru.practicum.model.EventSimilarityEntity;
import ru.practicum.model.EventToRecommend;
import ru.practicum.model.UserEventAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendedService {
    private final UserActionRepository userActionRepo;
    private final EventSimilarityRepository similarityRepo;

    public List<EventToRecommend> getSimilarEvents(SimilarEventsRequestProto request) {
        long eventId = request.getEventId();
        long userId = request.getUserId();
        int maxRes = request.getMaxResults();

        Set<Long> interacted = userInteracted(userId);
        List<EventToRecommend> result, recList = new ArrayList<>();

        similarityRepo.findByEventAOrEventB(eventId, eventId)
                .forEach(e -> {
                    long other = (e.getEventA() == eventId) ? e.getEventB() : e.getEventA();
                    if (!interacted.contains(other)) {
                        recList.add(new EventToRecommend(other, e.getScore()));
                    }
                });
        result = recList.stream()
                .sorted(Comparator.comparingDouble(EventToRecommend::score).reversed()).toList();

        return result.size() <= maxRes ? result : result.subList(0, maxRes);
    }

    public List<EventToRecommend> getRecommendationsForUser(UserPredictionsRequestProto request) {
        long userId = request.getUserId();
        int maxRes = request.getMaxResults();

        List<UserEventAction> all = userActionRepo.findByUserId(userId);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        all.sort((a, b) -> b.getLastInteraction().compareTo(a.getLastInteraction()));

        int min = Math.min(5, all.size());
        List<UserEventAction> recent = all.subList(0, min);

        Set<Long> interacted = userInteracted(userId);

        Map<Long, Double> bestScoreMap = new HashMap<>();
        for (UserEventAction r : recent) {
            long ev = r.getEventId();
            List<EventSimilarityEntity> simList = similarityRepo.findByEventAOrEventB(ev, ev);
            for (EventSimilarityEntity e : simList) {
                long other = (e.getEventA() == ev) ? e.getEventB() : e.getEventA();
                if (interacted.contains(other)) {
                    continue;
                }
                double oldVal = bestScoreMap.getOrDefault(other, 0.0);
                if (e.getScore() > oldVal) {
                    bestScoreMap.put(other, e.getScore());
                }
            }
        }

        return bestScoreMap.entrySet().stream()
                .map(e -> new EventToRecommend(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingDouble(EventToRecommend::score).reversed())
                .limit(maxRes)
                .collect(Collectors.toList());
    }

    public List<EventToRecommend> getInteractionsCount(InteractionsCountRequestProto request) {
        List<Long> events = request.getEventIdList();
        List<EventToRecommend> result = new ArrayList<>();

        for (Long e : events) {
            List<UserEventAction> list = userActionRepo.findByEventId(e);
            double sum = 0.0;
            for (UserEventAction uae : list) {
                sum += uae.getMaxWeight();
            }
            result.add(new EventToRecommend(e, (float) sum));
        }
        return result;
    }

    private Set<Long> userInteracted(long userId) {
        return userActionRepo.findByUserId(userId)
                .stream()
                .map(UserEventAction::getEventId)
                .collect(Collectors.toSet());
    }
}