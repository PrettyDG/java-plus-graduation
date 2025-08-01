package ru.practicum.service;



import ru.practicum.category.CategoryDto;
import ru.practicum.category.CategoryUpdateDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto categoryDto);

    List<CategoryDto> getAll();

    CategoryDto getById(Long id);

    void deleteById(Long id);

    List<CategoryDto> getAllPaged(int from, int size);

    CategoryDto update(Long catId, CategoryUpdateDto dto);
}
