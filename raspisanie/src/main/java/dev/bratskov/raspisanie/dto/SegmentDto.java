package dev.bratskov.raspisanie.dto;

import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Transport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalTime;

@Builder
@Schema(description = "Сегмент маршрута (участок на одном транспорте)")
public record SegmentDto(

        @Schema(description = "Остановка посадки")
        Stop boardingStop,

        @Schema(description = "Время посадки")
        LocalTime boardingTime,

        @Schema(description = "Остановка высадки")
        Stop exitStop,

        @Schema(description = "Время высадки")
        LocalTime exitTime,

        @Schema(description = "Тип транспорта")
        Transport transport,

        @Schema(description = "Номер маршрута")
        Integer routeNumber,

        @Schema(description = "Направление")
        String direction,

        @Schema(description = "Количество промежуточных остановок")
        Integer stopsCount
) {
}
