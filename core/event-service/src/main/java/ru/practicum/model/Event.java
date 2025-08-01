package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.category.CategoryDto;
import ru.practicum.event.EventState;
import ru.practicum.user.UserDto;


import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode()
@Builder
public class Event {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "annotation", columnDefinition = "TEXT")
    private String annotation;

    @JoinColumn(name = "category_id")
    private Long categoryId;

    @Column(name = "paid")
    private Boolean paid;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @JoinColumn(name = "initiator_id")
    private Long initiatorId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "participant_limit")
    private Integer participantLimit;

    @Enumerated(EnumType.STRING)
    private EventState state;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "request_moderation")
    private Boolean requestModeration;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "confirmed_requests")
    private Integer confirmedRequests;
}
