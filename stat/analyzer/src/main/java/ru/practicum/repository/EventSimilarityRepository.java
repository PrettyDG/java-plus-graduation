package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.EventSimilarityEntity;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    List<EventSimilarityEntity> findByEventAOrEventB(Long eventA, Long eventB);
}