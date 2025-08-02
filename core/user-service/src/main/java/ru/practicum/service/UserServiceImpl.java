package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.exceptions.ConflictException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;
import ru.practicum.user.UserDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserDto create(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Email уже существует: " + userDto.getEmail());
        }

        User user = userRepository.save(UserMapper.toEntity(userDto));
        return UserMapper.toDto(user);
    }

    @Override
    public List<UserDto> getAll(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return userRepository.findAll(pageable)
                .stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDto> getById(Long id) {
        List<UserDto> userDtos = userRepository.findById(id).stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
        log.info("Возвращаем - " + userDtos);
        return userDtos;
    }

    @Override
    public UserDto getUser(Long id) {
        UserDto userDto = UserMapper.toDto(userRepository.findById(id).get());
        log.info("Возвращаем - " + userDto);
        return userDto;
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        userRepository.deleteById(userId);
    }

    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
}