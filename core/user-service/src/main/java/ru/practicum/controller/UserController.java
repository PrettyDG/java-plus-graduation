package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.UserService;
import ru.practicum.user.UserDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody UserDto userDto) {
        log.info("createUser у UserController - " + userDto);
        UserDto userDto1 = userService.create(userDto);
        log.info("created userDto1 - " + userDto1);
        return userDto1;
    }

    @GetMapping("/exists/{id}")
    public boolean existsById(@PathVariable Long id) {
        return userService.existsById(id);
    }

    @GetMapping
    public List<UserDto> getAllUsers(
            @RequestParam(name = "from", defaultValue = "0") @Min(0) int from,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) int size
    ) {
        return userService.getAll(from, size);
    }

    @GetMapping(params = "ids")
    public ResponseEntity<List<UserDto>> getUserById(@RequestParam Long ids) {
        log.info("getUserById запрос к UserController - " + ids);
        return ResponseEntity.ok(userService.getById(ids));
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable("id") Long userId) {
        log.info("getUser у UserController - " + userId);
        return userService.getUser(userId);
    }


    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
    }
}