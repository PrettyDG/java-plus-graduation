package ru.practicum.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    @NotNull
    List<Long> requestIds;
    @NotNull
    RequestStatus status;
}
