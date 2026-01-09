package com.example.telegram_bot.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Stop(
        UUID id,
        String name,
        String description
) {
}
