package com.example.telegram_bot.dto;

import com.example.telegram_bot.model.Transport;
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
