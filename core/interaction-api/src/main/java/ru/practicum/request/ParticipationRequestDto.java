package ru.practicum.request;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
