package dev.bratskov.raspisanie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Результат поиска маршрута")
public record RoutingResponseDto(
        @Schema(description = "Общее количество уникальных остановок")
        Integer totalStops,

        @Schema(description = "Количество пересадок")
        Integer transfers,

        @Schema(description = "Общее время в пути (формат HH:mm)")
        String routeTime,

        @Schema(description = "Полный список всех остановок в маршруте")
        List<PathDto> fullPath,

        @Schema(description = "Краткая версия: только точки посадки, пересадки и выхода")
        List<SegmentDto> segments
) {
}
