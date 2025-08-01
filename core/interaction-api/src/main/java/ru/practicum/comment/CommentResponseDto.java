package ru.practicum.comment;

import lombok.Builder;
import lombok.Data;
import ru.practicum.event.EventShortDto;


import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponseDto {
    private Long id;
    private String text;
    private Long authorId;
    private EventShortDto event;
    private LocalDateTime created;
}
