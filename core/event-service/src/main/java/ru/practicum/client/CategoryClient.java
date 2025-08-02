package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.category.CategoryDto;

@FeignClient(name = "category-service", path = "/categories")
public interface CategoryClient {
    @GetMapping("/{id}")
    CategoryDto getCategoryById(@PathVariable Long id);
}