package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.event.*;
import ru.practicum.request.EventRequestStatusUpdateRequest;
import ru.practicum.request.ParticipationRequestDto;


import java.util.List;
import java.util.Map;

public interface EventService {

    EventShortDto getEventShort(Long id);

    List<EventShortDto> getEventsShortDto(List<Long> ids);

    List<EventShortDto> getUserEvents(Long userId,
                                      Pageable pageable);

    EventFullDto createEvent(Long userId,
                             NewEventDto newEventDto);

    EventFullDto getUserEventById(Long userId,
                                  Long eventId);

    EventFullDto updateUserEvent(Long userId,
                                 Long eventId,
                                 UpdateEventUserRequest updateEventUserRequest);

    List<ParticipationRequestDto> getEventRequests(Long userId,
                                                   Long eventId);

    Map<String, List<ParticipationRequestDto>> approveRequests(Long userId,
                                                               Long eventId,
                                                               EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest);

    List<EventFullDto> searchEventsByAdmin(SearchAdminEventsParamDto searchAdminEventsParamDto);

    EventFullDto updateEventByAdmin(Long eventId,
                                    UpdateEventAdminRequest updateEventAdminRequest);

    List<EventShortDto> searchPublicEvents(SearchPublicEventsParamDto searchPublicEventsParamDto);

    EventFullDto getPublicEvent(Long eventId,
                                HttpServletRequest request);
}
