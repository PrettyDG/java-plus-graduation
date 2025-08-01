package ru.practicum.mapper;


import ru.practicum.model.Request;
import ru.practicum.request.ParticipationRequestDto;

public class RequestMapper {

    public static ParticipationRequestDto toRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .requesterId(request.getRequesterId())
                .event(request.getEventId())
                .status(request.getStatus().getName())
                .created(request.getCreated())
                .build();
    }
}