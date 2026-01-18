package dev.bratskov.raspisanie.dto;

import dev.bratskov.raspisanie.model.enums.Day;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalTime;
import java.util.UUID;

@Builder
public record RoutingRequestDto(
        @NotNull(message = "ID начальной остановки обязателен")
        @Schema(description = "ID начальной остановки")
        UUID originStopId,

        @NotNull(message = "ID целевой остановки обязателен")
        @Schema(description = "ID целевой остановки")
        UUID targetStopId,

        @NotNull(message = "Время отправления обязательно")
        @Schema(
                description = "Время отправления в формате ISO 8601 (HH:mm:ss). " +
                        "**ПРИМЕР: 08:30:00**",
                type = "string",
                format = "time",
                example = "08:30:00"
        )
        LocalTime time,

        @NotNull(message = "День недели обязателен")
        @Schema(
                description = "Тип дня (будни или выходные)",
                example = "WEEKDAY",
                allowableValues = {"WEEKDAY", "WEEKEND"}
        )
        Day day
) {
}