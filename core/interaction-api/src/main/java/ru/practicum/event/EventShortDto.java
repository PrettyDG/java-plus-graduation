package ru.practicum.event;

import lombok.Builder;
import lombok.Data;
import ru.practicum.category.CategoryDto;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;

@Data
@Builder
public class EventShortDto {
    private String annotation;
    private CategoryDto categoryId;
    private Integer confirmedRequests;

    private LocalDateTime eventDate;
    private Long id;
    private UserDto initiatorId;
    private Boolean paid;
    private String title;
    private Long views;
}
