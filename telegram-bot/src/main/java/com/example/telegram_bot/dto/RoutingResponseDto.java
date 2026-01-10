package com.example.telegram_bot.dto;


import com.example.telegram_bot.model.enums.OptimizationType;
import lombok.Builder;

import java.util.List;

@Builder
public record RoutingResponseDto(
        Integer totalStops,

        Integer transfers,

        String routeTime,

        OptimizationType optimizationType,

        List<PathDto> fullPath,

        List<SegmentDto> segments
) {
}
