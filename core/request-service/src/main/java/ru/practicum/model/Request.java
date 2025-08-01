package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.event.EventFullDto;
import ru.practicum.user.UserDto;


import java.time.LocalDateTime;

@Entity
@Table(name = "participation_requests")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode()
@Builder
public class Request {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "requester_id")
    private Long requesterId;

    @JoinColumn(name = "event_id")
    private Long eventId;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private RequestStatusEntity status;

    @Column(name = "created")
    private LocalDateTime created;
}

