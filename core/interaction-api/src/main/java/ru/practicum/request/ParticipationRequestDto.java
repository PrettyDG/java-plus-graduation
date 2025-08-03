package ru.practicum.request;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
public class ParticipationRequestDto {

    LocalDateTime created;
    Long event;
    Long id;
    Long requester;
    RequestStatus status;
}
