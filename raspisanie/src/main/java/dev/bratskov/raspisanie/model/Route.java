package dev.bratskov.raspisanie.model;

import dev.bratskov.raspisanie.model.enums.Transport;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record Route(
        UUID id,
        Transport transport,
        Integer number,
        String direction,
        List<Stop> stops
) {
}
