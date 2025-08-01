package ru.practicum.compilation;

import lombok.Builder;
import lombok.Data;
import ru.practicum.event.EventShortDto;


import java.util.List;

@Data
@Builder
public class CompilationDto {
    private Long id;
    private String title;
    private boolean pinned;
    private List<Long> events;
}
