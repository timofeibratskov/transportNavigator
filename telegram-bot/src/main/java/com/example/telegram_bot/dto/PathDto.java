package com.example.telegram_bot.dto;

import com.example.telegram_bot.model.Stop;
import com.example.telegram_bot.model.Transport;
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
