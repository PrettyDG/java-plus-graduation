package ru.practicum.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import ru.practicum.category.CategoryDto;
import ru.practicum.user.UserDto;


import java.time.LocalDateTime;

@Data
@Builder
public class EventShortDto {
    private String annotation;
    private Long categoryId;
    private Integer confirmedRequests;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    private Long id;
    private Long initiatorId;
    private Boolean paid;
    private String title;
    private Long views;
}
