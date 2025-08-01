package ru.practicum.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.request.RequestStatus;

@Entity
@Table(name = "request_statuses")
@Getter
@Setter
@NoArgsConstructor
public class RequestStatusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RequestStatus name;
}
