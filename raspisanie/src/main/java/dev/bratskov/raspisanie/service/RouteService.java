package dev.bratskov.raspisanie.service;

import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.exception.ResourceNotFoundException;
import dev.bratskov.raspisanie.mapper.RouteMapper;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.repo.RouteRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RouteService {
    private final RouteRepo routeRepo;
    private final RouteMapper routeMapper;

    public List<ShortRouteDto> findAll(Transport type) {
        return routeRepo.getRoutes().stream()
                .filter(route -> type == null || route.transport().equals(type))
                .map(routeMapper::toShortRouteDto)
                .sorted(Comparator.comparing(ShortRouteDto::number))
                .toList();
    }

    public Route findById(UUID id) {
        return routeRepo.getRoutes().stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .orElseThrow(()
                        -> new ResourceNotFoundException("Маршрут с id=" + id + " не найден"));
    }

    public List<Route> findByNumber(int number) {
        return routeRepo.getRoutes()
                .stream()
                .filter(r -> r.number().equals(number))
                .toList();
    }

    public List<Route> findByNumber(int number, Transport type) {
        if (type != null) {
            return routeRepo.getRoutes().stream()
                    .filter(r -> r.number().equals(number)
                            && r.transport().equals(type))
                    .toList();
        } else {
            return this.findByNumber(number);
        }
    }

    public List<ShortRouteDto> findByStopId(UUID stopId) {
        return routeRepo.getRoutes().stream()
                .filter(route
                        -> route.stops()
                        .stream()
                        .anyMatch(stop
                                -> stop.id().equals(stopId)
                        )
                )
                .map(routeMapper::toShortRouteDto)
                .sorted(Comparator.comparing(ShortRouteDto::number))
                .toList();
    }
}


