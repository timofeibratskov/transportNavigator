package com.example.telegram_bot.dto;

import com.example.telegram_bot.model.Stop;
import com.example.telegram_bot.model.enums.Transport;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record SegmentDto(
        Stop boardingStop,
        LocalTime boardingTime,
        Stop exitStop,
        LocalTime exitTime,
        Transport transport,
        Integer routeNumber,
        String direction,
        Integer stopsCount
) {
}
