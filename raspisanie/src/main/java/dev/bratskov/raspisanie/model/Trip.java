package dev.bratskov.raspisanie.model;

import dev.bratskov.raspisanie.model.enums.Day;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record Trip(
        UUID id,
        Route route,
        List<StopTime> stops,
        Day day
        ) {
}
