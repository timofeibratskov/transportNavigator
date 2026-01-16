package dev.bratskov.raspisanie.mapper;

import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.model.Route;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public ShortRouteDto toShortRouteDto(Route route) {
        return ShortRouteDto.builder()
                .id(route.id())
                .number(route.number())
                .direction(route.direction())
                .transport(route.transport())
                .build();
    }
}
