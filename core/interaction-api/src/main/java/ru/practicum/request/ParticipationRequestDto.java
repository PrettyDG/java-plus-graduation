package ru.practicum.request;


import lombok.*;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
public class ParticipationRequestDto {

    LocalDateTime created;
    Long event;
    Long id;
    UserDto requester;
    RequestStatus status;
}
