package ru.practicum.mapper;


import ru.practicum.comment.CommentRequestDto;
import ru.practicum.comment.CommentResponseDto;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventShortDto;
import ru.practicum.model.Comment;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;

public class CommentMapper {

    public static Comment toComment(CommentRequestDto commentRequestDto,
                                    UserDto user,
                                    EventFullDto event) {
        return Comment.builder()
                .text(commentRequestDto.getText())
                .created(LocalDateTime.now())
                .authorId(user.getId())
                .eventId(event.getId())
                .build();
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment, EventShortDto eventShortDto) {

        return CommentResponseDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthorId())
                .event(eventShortDto)
                .created(comment.getCreated())
                .build();
    }
}
