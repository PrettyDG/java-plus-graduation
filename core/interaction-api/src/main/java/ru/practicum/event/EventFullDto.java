package ru.practicum.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import ru.practicum.category.CategoryDto;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;

@Data
@Builder
public class EventFullDto {
    private String annotation;
    private CategoryDto category;
    private Integer confirmedRequests;

    private LocalDateTime createdOn;
    private String description;

    private LocalDateTime eventDate;
    private Long id;
    private UserDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;

    private LocalDateTime publishedOn;
    private Boolean requestModeration;
    private EventState state;
    private String title;
    private Long views;
}
