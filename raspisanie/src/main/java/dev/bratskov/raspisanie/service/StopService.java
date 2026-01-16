package dev.bratskov.raspisanie.service;

import dev.bratskov.raspisanie.exception.ResourceNotFoundException;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.repo.StopRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StopService {
    private final StopRepo stopRepo;
    private final RouteService routeService;
    private final StopSearchService stopSearchService;

    public List<Stop> findAll() {
        return stopRepo.getStops().stream()
                .sorted(Comparator.comparing(Stop::name))
                .toList();
    }

    public List<Stop> findByName(String name) {
        return stopSearchService.searchByName(name);
    }

    public Stop findById(UUID id) {
        var stop = stopRepo.getStopById(id);
        if (stop == null) {
            throw new ResourceNotFoundException("остановка с id: " + id + " не найдена");
        } else {
            return stop;
        }
    }

    public List<List<Stop>> findByRouteIdAndTransportType(int number, Transport type) {
        return routeService.findByNumber(number, type)
                .stream()
                .map(Route::stops)
                .toList();
    }
}
