package ru.practicum.mapper;


import lombok.experimental.UtilityClass;
import ru.practicum.event.LocationDto;
import ru.practicum.model.Location;

@UtilityClass
public class LocationMapper {

    public static LocationDto toDto(Location location) {
        if (location == null) return null;
        return new LocationDto(location.getLat(), location.getLon());
    }

    public static Location toLocation(LocationDto dto) {
        if (dto == null) return null;
        Location location = new Location();
        location.setLat(dto.getLat());
        location.setLon(dto.getLon());
        return location;
    }
}