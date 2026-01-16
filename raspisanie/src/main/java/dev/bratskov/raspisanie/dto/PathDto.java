package dev.bratskov.raspisanie.dto;

import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Transport;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record PathDto(
        Stop stop,
        LocalTime time,
        String directionName,
        int number,
        Transport transport
) {
}
