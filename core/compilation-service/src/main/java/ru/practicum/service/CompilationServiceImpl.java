package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.compilation.CompilationDto;
import ru.practicum.compilation.NewCompilationDto;
import ru.practicum.compilation.UpdateCompilationRequest;
import ru.practicum.event.EventShortDto;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.repository.CompilationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto dto) {
        List<Long> events = dto.getEvents() == null
                ? Collections.emptyList()
                : new ArrayList<>(dto.getEvents());

        Compilation compilation = CompilationMapper.toEntity(dto, events);
        Compilation saved = compilationRepository.save(compilation);
        List<EventShortDto> eventShortDtoList = eventClient.getEventsShortDto(saved.getEventsId());
        return CompilationMapper.toDto(saved, eventShortDtoList);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            compilation.setEventsId(updateRequest.getEvents());
        }

        Compilation updated = compilationRepository.save(compilation);
        List<EventShortDto> eventShortDtoList = eventClient.getEventsShortDto(updated.getEventsId());
        return CompilationMapper.toDto(updated, eventShortDtoList);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<Compilation> all = compilationRepository.findAll(page).getContent();

        return all.stream()
                .filter(c -> pinned == null || c.isPinned() == pinned)
                .map(c -> {
                    List<Long> eventIds = c.getEventsId();
                    List<EventShortDto> eventShortDtoList = eventClient.getEventsShortDto(eventIds);
                    return CompilationMapper.toDto(c, eventShortDtoList);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
        List<EventShortDto> eventShortDtoList = eventClient.getEventsShortDto(compilation.getEventsId());
        return CompilationMapper.toDto(compilation, eventShortDtoList);
    }
}