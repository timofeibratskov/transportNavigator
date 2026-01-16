package com.example.telegram_bot.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RoutingResponseDto(
        Integer totalStops,

        Integer transfers,

        String routeTime,


        List<PathDto> fullPath,

        List<SegmentDto> segments
) {
}
