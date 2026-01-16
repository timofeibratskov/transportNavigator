package dev.bratskov.raspisanie.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Stop(
        UUID id,
        String name,
        String description
) {
}
