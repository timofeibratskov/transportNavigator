package com.example.telegram_bot.model;

import lombok.Builder;

import java.time.LocalTime;

@Builder
public record StopTime(
        Stop stop,
        LocalTime time
        ) {
}
