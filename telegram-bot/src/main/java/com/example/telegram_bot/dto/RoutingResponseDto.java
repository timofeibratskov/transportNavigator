package com.example.telegram_bot.dto;


import lombok.Builder;

import java.util.List;

@Builder
public record RoutingResponseDto(
        int transfers,
        int stopsAmount,
        String routeTime,
        List<PathDto> pathDtoList
) {
}
