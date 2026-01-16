package dev.bratskov.raspisanie.dto;

import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.enums.Transport;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record ScheduleResponseDto(
        int number,
        String direction,
        Transport transport,
        LocalTime time,
        Day day
) {
}
