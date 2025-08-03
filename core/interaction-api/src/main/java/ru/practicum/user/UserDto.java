package ru.practicum.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class UserDto {

    private Long id;

    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 250)
    private String name;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email must not be blank")
    @Size(min = 6, max = 254)
    private String email;
}
