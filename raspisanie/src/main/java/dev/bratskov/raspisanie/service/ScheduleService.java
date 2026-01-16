package dev.bratskov.raspisanie.service;

import dev.bratskov.raspisanie.dto.ScheduleResponseDto;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.repo.TripRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final RouteService routeService;
    private final StopService stopService;
    private final TripRepo tripRepo;

    public List<ScheduleResponseDto> findByStopIdAndRouteId(UUID stopId, UUID routeId, Day day) {
        try {
            var route = routeService.findById(routeId);
            var stop = stopService.findById(stopId);

            return tripRepo.getTrips()
                    .stream()
                    .filter(trip -> trip.route().equals(route))
                    .filter(trip -> day == null || trip.day().equals(day))
                    .flatMap(trip -> trip.stops().stream()
                            .filter(st -> st.stop().equals(stop) && st.time() != null)
                            .map(st -> ScheduleResponseDto.builder()
                                    .number(trip.route().number())
                                    .direction(trip.route().direction())
                                    .time(st.time())
                                    .transport(trip.route().transport())
                                    .day(trip.day()).build()
                            )
                    )
                    .sorted(Comparator.comparing(ScheduleResponseDto::time))
                    .toList();
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    public List<ScheduleResponseDto> findAllByStopId(UUID stopId, Day day) {
        try {
            var stop = stopService.findById(stopId);

            return tripRepo.getTrips().stream()
                    .filter(trip -> day == null || trip.day().equals(day))
                    .flatMap(trip -> trip.stops().stream()
                            .filter(st -> st.stop().equals(stop) && st.time() != null)
                            .map(st -> ScheduleResponseDto.builder()
                                    .number(trip.route().number())
                                    .direction(trip.route().direction())
                                    .transport(trip.route().transport())
                                    .time(st.time())
                                    .day(trip.day())
                                    .build()
                            )
                    )
                    .sorted(Comparator.comparing(ScheduleResponseDto::time))
                    .toList();
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }
}
