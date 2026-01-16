package dev.bratskov.raspisanie.service.raptor;

import dev.bratskov.raspisanie.dto.PathDto;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.StopTime;
import dev.bratskov.raspisanie.model.Trip;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.repo.StopRepo;
import dev.bratskov.raspisanie.repo.TripRepo;
import dev.bratskov.raspisanie.service.StopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.UUID;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaptorTest {

    @Mock
    private StopRepo stopRepo;
    @Mock
    private TripRepo tripRepo;
    @Mock
    private StopService stopService;

    private Raptor raptor;
    private Stop stopA, stopB, stopC, stopD;
    private Route route1, route2;

    @BeforeEach
    void setUp() {
        stopA = createStop("A");
        stopB = createStop("B");
        stopC = createStop("C");
        stopD = createStop("D");

        Map<String, Stop> stopMap = Map.of(
                "Desc A", stopA, "Desc B", stopB, "Desc C", stopC, "Desc D", stopD
        );
        when(stopRepo.getStopsMap()).thenReturn(stopMap);

        route1 = Route.builder().id(UUID.randomUUID()).number(1).transport(Transport.BUS).direction("C").build();
        route2 = Route.builder().id(UUID.randomUUID()).number(2).transport(Transport.BUS).direction("D").build();

        Trip trip1 = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 10, 0),
                        createStopTime(stopB, 10, 10),
                        createStopTime(stopC, 10, 20)
                )).build();

        Trip trip2 = Trip.builder()
                .id(UUID.randomUUID()).route(route2).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopC, 10, 30),
                        createStopTime(stopD, 10, 45)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(trip1, trip2));
        raptor = new Raptor(stopRepo, stopService, tripRepo);
    }

    @Test
    void plan_shouldFindDirectPath() {
        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopC.id())).thenReturn(stopC);

        List<PathDto> path = raptor.plan(stopA.id(), stopC.id(), LocalTime.of(9, 30), Day.WEEKDAY);

        assertThat(path).hasSize(3);
        assertThat(path.getLast().stop()).isEqualTo(stopC);
        assertThat(path.getLast().time()).isEqualTo(LocalTime.of(10, 20));
    }

    @Test
    void plan_shouldFindPathWithTransfer() {
        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopD.id())).thenReturn(stopD);

        List<PathDto> path = raptor.plan(stopA.id(), stopD.id(), LocalTime.of(9, 30), Day.WEEKDAY);

        assertThat(path).isNotEmpty();
        assertThat(path.getLast().stop()).isEqualTo(stopD);
        assertThat(path.stream().anyMatch(p -> p.stop().equals(stopC))).isTrue();
    }

    @Test
    void plan_shouldChooseEarlierTrip() {
        Trip earlierTrip = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 9, 0),
                        createStopTime(stopC, 9, 30)
                )).build();

        Trip trip1FromSetUp = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 10, 0),
                        createStopTime(stopB, 10, 10),
                        createStopTime(stopC, 10, 20)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(earlierTrip, trip1FromSetUp));
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopC.id())).thenReturn(stopC);

        List<PathDto> path = raptor.plan(stopA.id(), stopC.id(), LocalTime.of(8, 0), Day.WEEKDAY);

        assertThat(path.getLast().time()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void plan_shouldFailWhenTransferTimeTooShort() {
        Trip tripArrival = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 10, 0),
                        createStopTime(stopC, 10, 20)
                )).build();

        Trip fastTripDeparture = Trip.builder()
                .id(UUID.randomUUID()).route(route2).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopC, 10, 22),
                        createStopTime(stopD, 10, 40)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(tripArrival, fastTripDeparture));
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopD.id())).thenReturn(stopD);

        assertThatThrownBy(() -> raptor.plan(stopA.id(), stopD.id(), LocalTime.of(9, 0), Day.WEEKDAY))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void plan_shouldIgnoreWeekendTripsOnWeekday() {
        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopB.id())).thenReturn(stopB);

        assertThatThrownBy(() -> raptor.plan(stopA.id(), stopB.id(), LocalTime.of(8, 0), Day.WEEKEND))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void plan_shouldHandleNoTripsAfterStartTime() {
        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopB.id())).thenReturn(stopB);

        assertThatThrownBy(() -> raptor.plan(stopA.id(), stopB.id(), LocalTime.of(11, 0), Day.WEEKDAY))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void planAllPaths_shouldReturnUniquePaths() {
        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopC.id())).thenReturn(stopC);

        List<List<PathDto>> result = raptor.planAllPaths(stopA.id(), stopC.id(), LocalTime.of(9, 0), Day.WEEKDAY);

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst()).isNotEmpty();
    }

    @Test
    void plan_shouldThrowExceptionForSameOriginAndTarget() {
        assertThatThrownBy(() -> raptor.plan(stopA.id(), stopA.id(), LocalTime.now(), Day.WEEKDAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plan_shouldHandleEmptyTripRepo() {
        when(tripRepo.getTrips()).thenReturn(List.of());
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopB.id())).thenReturn(stopB);

        assertThatThrownBy(() -> raptor.plan(stopA.id(), stopB.id(), LocalTime.now(), Day.WEEKDAY))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void plan_shouldHandleCircularRoute() {
        Trip circularTrip = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 11, 0),
                        createStopTime(stopB, 11, 10),
                        createStopTime(stopC, 11, 20),
                        createStopTime(stopA, 11, 30)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(circularTrip));
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopC.id())).thenReturn(stopC);

        List<PathDto> path = raptor.plan(stopA.id(), stopC.id(), LocalTime.of(10, 0), Day.WEEKDAY);
        assertThat(path).hasSize(3);
        assertThat(path.getLast().time()).isEqualTo(LocalTime.of(11, 20));
    }

    @Test
    void plan_shouldHandleZeroTransferTimeAtLastStop() {
        Trip tripOut = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 10, 0),
                        createStopTime(stopB, 10, 20)
                )).build();

        Trip tripReturn = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopB, 10, 20), // Время совпадает секунда в секунду
                        createStopTime(stopA, 10, 40)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(tripOut, tripReturn));
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopB.id())).thenReturn(stopB);

        List<PathDto> path = raptor.plan(stopA.id(), stopB.id(), LocalTime.of(9, 0), Day.WEEKDAY);
        assertThat(path).isNotEmpty();
    }

    @Test
    void plan_shouldPickLaterDepartureIfItArrivesEarlier() {
        Trip slowTrip = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 10, 0),
                        createStopTime(stopC, 10, 40)
                )).build();

        Trip expressTrip = Trip.builder()
                .id(UUID.randomUUID()).route(route2).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 10, 20),
                        createStopTime(stopC, 10, 35)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(slowTrip, expressTrip));
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopC.id())).thenReturn(stopC);

        List<PathDto> path = raptor.plan(stopA.id(), stopC.id(), LocalTime.of(9, 0), Day.WEEKDAY);

        assertThat(path.getLast().time()).isEqualTo(LocalTime.of(10, 35));
    }

    @Test
    void planAllPaths_shouldThrowExceptionIfTargetNeverReached() {
        Stop isolated = createStop("Isolated");
        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(isolated.id())).thenReturn(isolated);

        assertThatThrownBy(() -> raptor.planAllPaths(stopA.id(), isolated.id(), LocalTime.of(8, 0), Day.WEEKDAY))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("не связаны транспортом");
    }

    @Test
    void plan_shouldWorkNearMidnight() {
        Trip nightTrip = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 23, 50),
                        createStopTime(stopB, 23, 59)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(nightTrip));
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopB.id())).thenReturn(stopB);

        List<PathDto> path = raptor.plan(stopA.id(), stopB.id(), LocalTime.of(23, 45), Day.WEEKDAY);
        assertThat(path).hasSize(2);
        assertThat(path.getLast().time()).isEqualTo(LocalTime.of(23, 59));
    }

    @Test
    void plan_shouldHandleRouteWithMultiplePassesOfSameStop() {
        Trip loopTrip = Trip.builder()
                .id(UUID.randomUUID()).route(route1).day(Day.WEEKDAY)
                .stops(List.of(
                        createStopTime(stopA, 10, 0),
                        createStopTime(stopB, 10, 10),
                        createStopTime(stopC, 10, 20),
                        createStopTime(stopB, 10, 30),
                        createStopTime(stopD, 10, 40)
                )).build();

        when(tripRepo.getTrips()).thenReturn(List.of(loopTrip));
        raptor = new Raptor(stopRepo, stopService, tripRepo);

        when(stopService.findById(stopA.id())).thenReturn(stopA);
        when(stopService.findById(stopD.id())).thenReturn(stopD);

        List<PathDto> path = raptor.plan(stopA.id(), stopD.id(), LocalTime.of(9, 0), Day.WEEKDAY);

        assertThat(path).hasSize(5);
        assertThat(path.getLast().stop()).isEqualTo(stopD);
    }


    private Stop createStop(String name) {
        return Stop.builder().id(UUID.randomUUID()).name(name).description("Desc " + name).build();
    }

    private StopTime createStopTime(Stop stop, int h, int m) {
        return StopTime.builder().stop(stop).time(LocalTime.of(h, m)).build();
    }
}