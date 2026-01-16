package dev.bratskov.raspisanie.controller;

import dev.bratskov.raspisanie.dto.RoutingRequestDto;
import dev.bratskov.raspisanie.dto.RoutingResponseDto;
import dev.bratskov.raspisanie.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routing")
@Tag(name = "ROUTING-CONTROLLER")
public class RoutingController {
    private final RoutingService routingService;

    @PostMapping("/plan")
    @Operation(
            summary = "Найти оптимальный маршрут",
            description = "Возвращает один оптимальный маршрут (самый быстрый по времени)"
    )
    public RoutingResponseDto getPlan(@Valid @RequestBody RoutingRequestDto requestDto) {
        return routingService.findPath(requestDto);
    }

    @PostMapping("/plan/all")
    @Operation(
            summary = "Найти все варианты маршрута",
            description = "Возвращает до 3 вариантов маршрута, оптимизированных по разным критериям: " +
                    "самый быстрый (FASTEST), с минимумом остановок (LEAST_STOPS), " +
                    "с минимумом пересадок (LEAST_TRANSFERS). " +
                    "Если варианты совпадают, возвращается только уникальные маршруты."
    )
    public List<RoutingResponseDto> getAllPlans(@Valid @RequestBody RoutingRequestDto requestDto) {
        return routingService.findAllPaths(requestDto);
    }
}
