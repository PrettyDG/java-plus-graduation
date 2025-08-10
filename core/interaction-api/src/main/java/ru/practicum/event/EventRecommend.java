package ru.practicum.event;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class EventRecommend {
    private Long eventId;
    private double rating;
}
