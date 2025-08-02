package ru.practicum.mapper;

import ru.practicum.model.User;
import ru.practicum.user.UserDto;


public class UserMapper {

    public static UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static User toEntity(UserDto userDto) {
        if (userDto == null) return null;

        return User.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();
    }
}
