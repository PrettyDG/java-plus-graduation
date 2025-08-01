package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.user.UserDto;

@FeignClient(name = "user-service", path = "/admin/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUser(@PathVariable("id") Long userId);

    @GetMapping("/exists/{id}")
    Boolean existsById(@PathVariable("id") Long userId);
}