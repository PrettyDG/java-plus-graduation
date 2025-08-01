package ru.practicum.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.event.EventFullDto;
import ru.practicum.model.Comment;
import ru.practicum.user.UserDto;


import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    void deleteByEventId(Long eventId);

    List<Comment> findByEventId(Long eventId, Pageable pageable);

    List<Comment> findByAuthorIdAndEventId(Long authorId, Long eventId, Pageable pageable);
}