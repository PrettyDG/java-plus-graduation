package ru.practicum.repository;

import feign.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.model.Event;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.categoryId = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);

    List<Event> findByInitiatorId(Long initiatorId,
                                  Pageable pageable);
}
