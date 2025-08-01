package ru.practicum.mapper;


import ru.practicum.category.CategoryDto;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventShortDto;
import ru.practicum.event.EventState;
import ru.practicum.event.NewEventDto;
import ru.practicum.model.Event;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;

public class EventMapper {

    public static Event toEvent(NewEventDto newEventDto,
                                UserDto initiator,
                                CategoryDto category) {

        return Event.builder()
                .initiatorId(initiator.getId())
                .annotation(newEventDto.getAnnotation())
                .categoryId(category.getId())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(LocationMapper.toLocation(newEventDto.getLocation()))
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .confirmedRequests(0)
                .build();
    }

    public static EventShortDto toShortDto(Event event) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .categoryId(event.getCategoryId())
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(0L)
                .build();
    }

    public static EventFullDto toFullDto(Event event) {
        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .categoryId(event.getCategoryId())
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .location(LocationMapper.toDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(0L)
                .build();
    }
}
