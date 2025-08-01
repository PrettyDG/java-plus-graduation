package ru.practicum.mapper;


import ru.practicum.model.Request;
import ru.practicum.model.RequestStatusEntity;
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

    public static Request toEntity(ParticipationRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Request request = new Request();

        request.setId(dto.getId());
        request.setRequesterId(dto.getRequesterId());
        request.setEventId(dto.getEvent());
        request.setCreated(dto.getCreated());

        if (dto.getStatus() != null) {
            RequestStatusEntity statusEntity = new RequestStatusEntity();
            statusEntity.setName(dto.getStatus());
            request.setStatus(statusEntity);
        }

        return request;
    }
}