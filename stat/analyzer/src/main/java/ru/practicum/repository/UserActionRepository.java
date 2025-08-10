package ru.practicum.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.UserEventAction;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserEventAction, Long> {

    UserEventAction findByUserIdAndEventId(Long userId, Long eventId);

    List<UserEventAction> findByUserId(Long userId);

    List<UserEventAction> findByEventId(Long eventId);
}