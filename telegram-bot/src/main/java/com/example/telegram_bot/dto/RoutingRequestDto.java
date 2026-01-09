package com.example.telegram_bot.dto;


import com.example.telegram_bot.model.Day;
import lombok.Builder;

import java.time.LocalTime;
import java.util.UUID;

@Builder
public record RoutingRequestDto(
        UUID originStopId,
        UUID targetStopId,
        LocalTime time,
        Day day
) {
}