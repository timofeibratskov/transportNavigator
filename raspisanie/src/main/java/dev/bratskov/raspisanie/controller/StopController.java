package dev.bratskov.raspisanie.controller;

import dev.bratskov.raspisanie.dto.ScheduleResponseDto;
import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.service.RouteService;
import dev.bratskov.raspisanie.service.ScheduleService;
import dev.bratskov.raspisanie.service.StopService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stops")
@Tag(name = "STOP-CONTROLLER")
public class StopController {
    private final StopService stopService;
    private final RouteService routeService;
    private final ScheduleService scheduleService;

    @GetMapping("/{id}")
    public ResponseEntity<Stop> getStopById(@PathVariable UUID id) {
        return ResponseEntity.ok(stopService.findById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Stop>> searchStops(@RequestParam String name) {
        return ResponseEntity.ok(stopService.findByName(name));
    }

    @GetMapping
    public ResponseEntity<List<Stop>> getAllStops() {
        return ResponseEntity.ok(stopService.findAll());
    }

    @GetMapping("/{id}/routes")
    public ResponseEntity<List<ShortRouteDto>> getAllRoutesByStop(@PathVariable UUID id) {
        return ResponseEntity.ok(routeService.findByStopId(id));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<List<ScheduleResponseDto>> getAllRoutesScheduleByStop(@PathVariable UUID id, @RequestParam(required = false) Day day) {
        return ResponseEntity.ok(scheduleService.findAllByStopId(id, day));
    }

    @GetMapping("/{stopId}/schedule/route/{routeId}")
    public ResponseEntity<List<ScheduleResponseDto>> getSpecificRouteScheduleByStop(
            @PathVariable UUID stopId,
            @PathVariable UUID routeId,
            @RequestParam(required = false) Day day) {
        return ResponseEntity.ok(scheduleService.findByStopIdAndRouteId(stopId, routeId, day));
    }
}