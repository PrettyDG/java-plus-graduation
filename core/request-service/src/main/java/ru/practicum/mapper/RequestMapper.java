package ru.practicum.mapper;


import lombok.experimental.UtilityClass;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatusEntity;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.user.UserDto;

@UtilityClass
public class RequestMapper {

    public static ParticipationRequestDto toRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .requester(request.getRequesterId())
                .event(request.getEventId())
                .status(request.getStatus().getName())
                .created(request.getCreated())
                .build();
    }

    public static Request toEntity(ParticipationRequestDto dto, RequestStatusEntity requestStatusEntity) {
        if (dto == null) {
            return null;
        }

        Request request = new Request();

        request.setId(dto.getId());
        request.setRequesterId(dto.getRequester());
        request.setEventId(dto.getEvent());
        request.setCreated(dto.getCreated());
        request.setStatus(requestStatusEntity);

        return request;
    }
}