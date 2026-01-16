package dev.bratskov.raspisanie.controller;

import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.mapper.RouteMapper;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.service.RouteService;
import dev.bratskov.raspisanie.service.StopService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routes")
@Tag(name = "ROUTE-CONTROLLER")
public class RouteController {
    private final RouteService routeService;
    private final StopService stopService;
    private final RouteMapper routeMapper;

    @GetMapping
    public ResponseEntity<List<ShortRouteDto>> getAllRoutes(@RequestParam(required = false) Transport type) {
        return ResponseEntity.ok(routeService.findAll(type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRouteById(@PathVariable UUID id) {
        return ResponseEntity.ok(routeService.findById(id));
    }

    @GetMapping("/number/{number}")
    public ResponseEntity<List<ShortRouteDto>> getRoutesByNumber(@PathVariable int number, @RequestParam(required = false) Transport type) {
        return ResponseEntity.ok(routeService.findByNumber(number, type).
                stream()
                .map(routeMapper::toShortRouteDto)
                .toList());
    }

    @GetMapping("number/{number}/stops")
    public ResponseEntity<List<List<Stop>>> getStopsByRoute(@PathVariable int number, @RequestParam Transport type) {
        return ResponseEntity.ok(stopService.findByRouteIdAndTransportType(number, type));
    }
}
