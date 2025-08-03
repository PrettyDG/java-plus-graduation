package ru.practicum.mapper;


import lombok.experimental.UtilityClass;
import ru.practicum.category.CategoryDto;
import ru.practicum.model.Category;

@UtilityClass
public class CategoryMapper {
    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category toEntity(CategoryDto dto) {
        return Category.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }
}