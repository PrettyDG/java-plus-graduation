package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.compilation.CompilationDto;
import ru.practicum.compilation.NewCompilationDto;
import ru.practicum.event.EventShortDto;
import ru.practicum.model.Compilation;

import java.util.List;

@UtilityClass
public class CompilationMapper {

    public CompilationDto toDto(Compilation compilation, List<EventShortDto> eventShortDtoList) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(eventShortDtoList)
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