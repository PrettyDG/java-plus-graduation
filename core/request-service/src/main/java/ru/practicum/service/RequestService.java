package ru.practicum.service;


import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> findByEventId(Long eventId);

    List<ParticipationRequestDto> findByIds(List<Long> ids);

    List<ParticipationRequestDto> saveAll(List<ParticipationRequestDto> requestDtos);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createParticipationRequest(Long userId,
                                                       Long eventId);

    ParticipationRequestDto cancelParticipationRequest(Long userId,
                                                       Long requestId);
}
