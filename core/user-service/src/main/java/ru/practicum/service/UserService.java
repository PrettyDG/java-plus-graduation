package ru.practicum.service;

import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(UserDto userDto);

    List<UserDto> getAll(int from, int size);

    UserDto getUser(Long id);

    List<UserDto> getById(Long id);

    void deleteUser(Long userId);

    boolean existsById(@PathVariable Long id);
}
