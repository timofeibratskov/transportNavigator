package com.example.telegram_bot.client;

import com.example.telegram_bot.dto.RoutingResponseDto;
import com.example.telegram_bot.dto.RoutingRequestDto;
import com.example.telegram_bot.dto.ShortRouteDto;
import com.example.telegram_bot.dto.ScheduleResponseDto;
import com.example.telegram_bot.model.Day;
import com.example.telegram_bot.model.Route;
import com.example.telegram_bot.model.Stop;
import com.example.telegram_bot.model.Transport;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "raspisanie-service")
public interface RaspisanieClient {

    @GetMapping("/api/v1/stops")
    List<Stop> getAllStops();

    @GetMapping("/api/v1/stops/search")
    List<Stop> searchStops(@RequestParam String name);

    @GetMapping("/api/v1/stops/{id}")
    Stop getStopById(@PathVariable UUID id);

    @GetMapping("/api/v1/stops/{id}/routes")
    List<ShortRouteDto> getRoutesByStop(@PathVariable UUID id);

    @GetMapping("/api/v1/stops/{id}/schedule")
    List<ScheduleResponseDto> getStopSchedule(
            @PathVariable UUID id,
            @RequestParam(required = false) Day day
    );

    @GetMapping("/api/v1/routes")
    List<ShortRouteDto> getAllRoutes(@RequestParam(required = false) Transport type);

    @GetMapping("/api/v1/routes/{id}")
    Route getRouteById(@PathVariable UUID id);

    @GetMapping("/api/v1/routes/number/{number}")
    List<ShortRouteDto> getRoutesByNumber(
            @PathVariable int number,
            @RequestParam(required = false) Transport type
    );

    @PostMapping("/api/v1/routing/plan")
    RoutingResponseDto planRoute(@RequestBody RoutingRequestDto request);
}