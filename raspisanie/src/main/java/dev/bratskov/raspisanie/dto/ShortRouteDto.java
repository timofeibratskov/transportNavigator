package dev.bratskov.raspisanie.dto;

import dev.bratskov.raspisanie.model.enums.Transport;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ShortRouteDto(
        UUID id,
        int number,
        String direction,
        Transport transport
) {
}
