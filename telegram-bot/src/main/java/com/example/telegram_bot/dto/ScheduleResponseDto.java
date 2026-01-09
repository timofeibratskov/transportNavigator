package com.example.telegram_bot.dto;

import com.example.telegram_bot.model.Day;
import com.example.telegram_bot.model.Transport;
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
