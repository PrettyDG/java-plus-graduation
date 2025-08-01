package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.model.RequestStatusEntity;
import ru.practicum.request.RequestStatus;

import java.util.Optional;

@Repository
public interface RequestStatusRepository extends JpaRepository<RequestStatusEntity, Long> {
    Optional<RequestStatusEntity> findByName(RequestStatus name);
}
