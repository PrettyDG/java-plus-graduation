package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.springframework.stereotype.Service;
import ru.practicum.client.EventClient;
import ru.practicum.compilation.CompilationDto;
import ru.practicum.compilation.NewCompilationDto;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventShortDto;
import ru.practicum.model.Compilation;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation) {
        List<Long> eventIds = compilation.getEventsId();


        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(eventIds)
                .build();
    }

    public Compilation toEntity(NewCompilationDto dto, List<Long> events) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.isPinned())
                .eventsId(events)
                .build();
    }
}