package dev.bratskov.raspisanie.unit.service;

import dev.bratskov.raspisanie.dto.ScheduleResponseDto;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.StopTime;
import dev.bratskov.raspisanie.model.Trip;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.repo.TripRepo;
import dev.bratskov.raspisanie.service.RouteService;
import dev.bratskov.raspisanie.service.ScheduleService;
import dev.bratskov.raspisanie.service.StopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private RouteService routeService;
    @Mock
    private StopService stopService;
    @Mock
    private TripRepo tripRepo;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void findByStopIdAndRouteId_shouldReturnCorrectSchedule() {
        UUID stopId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        Stop targetStop = Stop.builder().id(stopId).name("ВИНЕВЕЦ").build();
        Route targetRoute = Route.builder()
                .id(routeId)
                .number(5)
                .transport(Transport.BUS)
                .build();

        when(routeService.findById(routeId)).thenReturn(targetRoute);
        when(stopService.findById(stopId)).thenReturn(targetStop);

        StopTime stopAt8 = StopTime.builder().stop(targetStop).time(LocalTime.of(8, 0)).build();
        StopTime stopAt10 = StopTime.builder().stop(targetStop).time(LocalTime.of(10, 30)).build();

        Trip trip = Trip.builder()
                .route(targetRoute)
                .day(Day.WEEKDAY)
                .stops(List.of(stopAt10, stopAt8))
                .build();

        when(tripRepo.getTrips()).thenReturn(List.of(trip));

        List<ScheduleResponseDto> result = scheduleService.findByStopIdAndRouteId(stopId, routeId, Day.WEEKDAY);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).time()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.get(1).time()).isEqualTo(LocalTime.of(10, 30));
    }

    @Test
    void findAllByStopId_shouldFilterOutOtherDays() {
        UUID stopId = UUID.randomUUID();
        Stop targetStop = Stop.builder().id(stopId).build();
        Route route = Route.builder().number(1).build();

        when(stopService.findById(stopId)).thenReturn(targetStop);

        Trip weekdayTrip = Trip.builder()
                .route(route)
                .day(Day.WEEKDAY)
                .stops(List.of(StopTime.builder().stop(targetStop).time(LocalTime.of(9, 0)).build()))
                .build();

        Trip weekendTrip = Trip.builder()
                .route(route)
                .day(Day.WEEKEND)
                .stops(List.of(StopTime.builder().stop(targetStop).time(LocalTime.of(10, 0)).build()))
                .build();

        when(tripRepo.getTrips()).thenReturn(List.of(weekdayTrip, weekendTrip));

        List<ScheduleResponseDto> result = scheduleService.findAllByStopId(stopId, Day.WEEKEND);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().day()).isEqualTo(Day.WEEKEND);
        assertThat(result.getFirst().time()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void shouldReturnEmptyList_whenExceptionOccurs() {
        UUID stopId = UUID.randomUUID();
        when(stopService.findById(stopId)).thenThrow(new IllegalArgumentException("Not found"));

        List<ScheduleResponseDto> result = scheduleService.findAllByStopId(stopId, Day.WEEKDAY);

        assertThat(result).isEmpty();
    }

    @Test
    void findByStopIdAndRouteId_shouldIgnoreOtherStopsInTrip() {
        UUID stopId = UUID.randomUUID();
        Stop targetStop = Stop.builder().id(stopId).build();
        Stop otherStop = Stop.builder().id(UUID.randomUUID()).build();
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(1)
                .stops(List.of(targetStop))
                .build();

        when(routeService.findById(any())).thenReturn(route);
        when(stopService.findById(stopId)).thenReturn(targetStop);

        StopTime ourStopTime = StopTime.builder().stop(targetStop).time(LocalTime.of(12, 0)).build();
        StopTime otherStopTime = StopTime.builder().stop(otherStop).time(LocalTime.of(12, 10)).build();

        Trip trip = Trip.builder()
                .route(route)
                .day(Day.WEEKDAY)
                .stops(List.of(ourStopTime, otherStopTime))
                .build();

        when(tripRepo.getTrips()).thenReturn(List.of(trip));

        List<ScheduleResponseDto> result = scheduleService.findByStopIdAndRouteId(stopId, route.id(), Day.WEEKDAY);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().time()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void findAllByStopId_shouldIgnoreNullTimes() {
        UUID stopId = UUID.randomUUID();
        Stop targetStop = Stop.builder().id(stopId).build();
        when(stopService.findById(stopId)).thenReturn(targetStop);

        StopTime validTime = StopTime.builder().stop(targetStop).time(LocalTime.of(15, 0)).build();
        StopTime nullTime = StopTime.builder().stop(targetStop).time(null).build();

        Trip trip = Trip.builder()
                .route(Route.builder()
                        .id(UUID.randomUUID())
                        .transport(Transport.BUS)
                        .number(1)
                        .stops(List.of(targetStop))
                        .build())
                .day(Day.WEEKDAY)
                .stops(List.of(validTime, nullTime))
                .build();

        when(tripRepo.getTrips()).thenReturn(List.of(trip));

        List<ScheduleResponseDto> result = scheduleService.findAllByStopId(stopId, Day.WEEKDAY);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().time()).isNotNull();
    }

    @Test
    void findAllByStopId_shouldReturnEverything_whenDayIsNull() {
        UUID stopId = UUID.randomUUID();
        Stop targetStop = Stop.builder().id(stopId).build();
        when(stopService.findById(stopId)).thenReturn(targetStop);

        Trip tripWeekday = Trip.builder()
                .day(Day.WEEKDAY)
                .route(Route.builder()
                        .stops(List.of(targetStop))
                        .number(1)
                        .transport(Transport.BUS)
                        .build())
                .stops(List.of(StopTime.builder().stop(targetStop).time(LocalTime.of(9, 0)).build())).build();
        Trip tripWeekend = Trip.builder()
                .day(Day.WEEKEND)
                .route(Route.builder()
                        .number(4)
                        .stops(List.of(targetStop))
                        .transport(Transport.TROLLEYBUS)
                        .build())
                .stops(List.of(StopTime.builder().stop(targetStop).time(LocalTime.of(10, 0)).build())).build();

        when(tripRepo.getTrips()).thenReturn(List.of(tripWeekday, tripWeekend));

        List<ScheduleResponseDto> result = scheduleService.findAllByStopId(stopId, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ScheduleResponseDto::day).containsExactlyInAnyOrder(Day.WEEKDAY, Day.WEEKEND);
    }
}