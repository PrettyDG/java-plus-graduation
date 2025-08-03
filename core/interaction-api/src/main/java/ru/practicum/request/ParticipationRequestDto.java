package ru.practicum.request;


import lombok.Builder;
import lombok.Data;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;

@Data
@Builder
public class ParticipationRequestDto {

    LocalDateTime created;
    Long event;
    Long id;
    UserDto requester;
    RequestStatus status;
}
