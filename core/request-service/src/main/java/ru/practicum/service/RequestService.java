package ru.practicum.service;


import ru.practicum.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createParticipationRequest(Long userId,
                                                       Long eventId);

    ParticipationRequestDto cancelParticipationRequest(Long userId,
                                                       Long requestId);
}
