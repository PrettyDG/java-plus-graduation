package ru.practicum.service;

import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.UserClient;
import ru.practicum.clients.EventClient;
import ru.practicum.comment.CommentRequestDto;
import ru.practicum.comment.CommentResponseDto;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventShortDto;
import ru.practicum.event.EventState;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.repository.CommentRepository;
import ru.practicum.user.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImpl implements CommentService {

    CommentRepository commentRepository;

    UserClient userClient;

    EventClient eventClient;

    @Override
    public List<CommentResponseDto> findAll(Long userId,
                                            Long eventId,
                                            PageRequest pageRequest) {
        EventShortDto eventShortDto = getShortEventById(eventId);
        List<Comment> comments = commentRepository.findByAuthorIdAndEventId(userId, eventId, pageRequest);
        return comments.stream()
                .map(comment -> CommentMapper.toCommentResponseDto(comment, eventShortDto))
                .toList();
    }

    @Override
    public CommentResponseDto save(CommentRequestDto commentRequestDto,
                                   Long userId,
                                   Long eventId) {
        UserDto user = getUserById(userId);
        EventFullDto event = getEventById(eventId);
        EventShortDto eventShortDto = getShortEventById(eventId);

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя написать комментарий к событию которое еще не было опубликованно");
        }
        Comment comment = commentRepository.save(CommentMapper.toComment(commentRequestDto, user, event));
        return CommentMapper.toCommentResponseDto(comment, eventShortDto);
    }

    @Override
    public CommentResponseDto update(CommentRequestDto commentRequestDto,
                                     Long userId,
                                     Long commentId) {
        Comment oldComment = getCommentById(commentId);
        EventShortDto eventShortDto = eventClient.getEventShort(oldComment.getEventId());
        getUserById(userId);

        if (!oldComment.getAuthorId().equals(userId)) {
            throw new ConflictException("Редактировать комментарии разрешено только его автору");
        }
        oldComment.setText(commentRequestDto.getText());
        Comment comment = commentRepository.save(oldComment);
        return CommentMapper.toCommentResponseDto(comment, eventShortDto);
    }

    @Override
    public void delete(Long userId,
                       Long commentId) {
        Comment comment = getCommentById(commentId);
        getUserById(userId);
        if (!comment.getAuthorId().equals(userId) &&
                !comment.getAuthorId().equals(comment.getEventId())) {
            throw new ConflictException("Удалять комментарии разрешено только его автору или инициатору мероприятия");
        }
        commentRepository.deleteById(commentId);
    }


    @Override
    public void deleteByIds(final List<Long> ids) {
        List<EventFullDto> events = eventClient.findAllById(ids);
        if (ids.size() != events.size()) {
            throw new ValidationException("Были переданы несуществующие id событий");
        }
        commentRepository.deleteAllById(ids);
        log.info("Комментарии успешно удалены");
    }

    @Override
    public void deleteByEventId(Long eventId) {
        commentRepository.deleteByEventId(eventId);
        log.info("Все комментарии у события с id = {} успешно удалены", eventId);
    }

    @Override
    public List<CommentResponseDto> findByEvent(Long eventId,
                                                PageRequest pageRequest) {
        EventShortDto eventShortDto = getShortEventById(eventId);
        List<Comment> comments = commentRepository.findByEventId(eventId, pageRequest);
        log.info("Получены все комментарии события с id = {}", eventId);

        return comments.stream()
                .map(comment -> CommentMapper.toCommentResponseDto(comment, eventShortDto))
                .toList();
    }

    @Override
    public CommentResponseDto findById(final Long commentId) {
        Comment comment = getCommentById(commentId);
        return CommentMapper.toCommentResponseDto(comment, eventClient.getEventShort(commentId));
    }

    private UserDto getUserById(Long userId) {
        try {
            return userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }

    private EventFullDto getEventById(Long eventId) {
        try {
            return eventClient.getEventById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("События с id = {} нет." + eventId);
        }
    }

    private EventShortDto getShortEventById(Long eventId) {
        try {
            return eventClient.getEventShort(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("События с id = {} нет." + eventId);
        }
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментария с id = {} нет." + commentId));
    }
}
