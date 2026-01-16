package dev.bratskov.raspisanie.service;

import dev.bratskov.raspisanie.dto.PathDto;
import dev.bratskov.raspisanie.dto.RoutingRequestDto;
import dev.bratskov.raspisanie.dto.RoutingResponseDto;
import dev.bratskov.raspisanie.dto.SegmentDto;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.service.raptor.Raptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private Raptor raptor;

    @InjectMocks
    private RoutingService routingService;

    @Test
    void findPath_shouldReturnRoutingResponse_whenPathExists() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 30);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        Stop stop3 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop C")
                .description("Description C")
                .build();

        List<PathDto> path = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 30))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(8, 45))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop3)
                        .time(LocalTime.of(9, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build()
        );

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(path);

        RoutingResponseDto result = routingService.findPath(request);

        assertThat(result).isNotNull();
        assertThat(result.fullPath()).hasSize(3);
        assertThat(result.totalStops()).isEqualTo(3);
        assertThat(result.transfers()).isEqualTo(0);
        assertThat(result.routeTime()).isEqualTo("00:30");
        assertThat(result.segments()).hasSize(1);

        verify(raptor).plan(originId, targetId, departureTime, day);
    }

    @Test
    void findPath_shouldCalculateDurationCorrectly_whenCrossesMidnight() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(23, 30);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<PathDto> path = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(23, 30))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(0, 15))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build()
        );

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(path);

        RoutingResponseDto result = routingService.findPath(request);

        assertThat(result.routeTime()).isEqualTo("00:45");
    }

    @Test
    void findPath_shouldCreateMultipleSegments_whenThereAreTransfers() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        Stop stop3 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop C")
                .description("Description C")
                .build();

        Stop stop4 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop D")
                .description("Description D")
                .build();

        List<PathDto> path = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(8, 15))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop3)
                        .time(LocalTime.of(8, 25))
                        .transport(Transport.TROLLEYBUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build(),
                PathDto.builder()
                        .stop(stop4)
                        .time(LocalTime.of(8, 40))
                        .transport(Transport.TROLLEYBUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build()
        );

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(path);

        RoutingResponseDto result = routingService.findPath(request);

        assertThat(result.segments()).hasSize(2);
        assertThat(result.transfers()).isEqualTo(1);

        SegmentDto segment1 = result.segments().getFirst();
        assertThat(segment1.boardingStop()).isEqualTo(stop1);
        assertThat(segment1.exitStop()).isEqualTo(stop2);
        assertThat(segment1.routeNumber()).isEqualTo(5);
        assertThat(segment1.stopsCount()).isEqualTo(2);

        SegmentDto segment2 = result.segments().get(1);
        assertThat(segment2.boardingStop()).isEqualTo(stop3);
        assertThat(segment2.exitStop()).isEqualTo(stop4);
        assertThat(segment2.routeNumber()).isEqualTo(10);
        assertThat(segment2.stopsCount()).isEqualTo(2);
    }

    @Test
    void findPath_shouldThrowException_whenPathIsEmpty() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(List.of());

        assertThatThrownBy(() -> routingService.findPath(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Путь не может быть пустым");
    }

    @Test
    void findAllPaths_shouldReturnMultiplePaths() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<PathDto> fastestPath = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(8, 15))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build()
        );

        List<PathDto> leastStopsPath = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 0))
                        .transport(Transport.BUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(8, 30))
                        .transport(Transport.BUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build()
        );

        List<List<PathDto>> paths = List.of(fastestPath, leastStopsPath);

        when(raptor.planAllPaths(originId, targetId, departureTime, day)).thenReturn(paths);

        List<RoutingResponseDto> results = routingService.findAllPaths(request);

        assertThat(results).hasSize(2);

        verify(raptor).planAllPaths(originId, targetId, departureTime, day);
    }

    @Test
    void findAllPaths_shouldHandleEmptyVariants() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        when(raptor.planAllPaths(originId, targetId, departureTime, day)).thenReturn(List.of());

        List<RoutingResponseDto> results = routingService.findAllPaths(request);

        assertThat(results).isEmpty();
    }

    @Test
    void findPath_shouldCalculateTotalStopsCorrectly() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<PathDto> path = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(8, 15))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 25))
                        .transport(Transport.TROLLEYBUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build()
        );

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(path);

        RoutingResponseDto result = routingService.findPath(request);

        assertThat(result.totalStops()).isEqualTo(2);
    }

    @Test
    void findPath_shouldFormatDurationCorrectly_forLongTrips() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(7, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<PathDto> path = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(7, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(9, 35))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build()
        );

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(path);

        RoutingResponseDto result = routingService.findPath(request);

        assertThat(result.routeTime()).isEqualTo("02:35");
    }

    @Test
    void findAllPaths_shouldSortByDuration_fastestFirst() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<PathDto> slowPath = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(9, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build()
        );

        // Быстрый путь (15 минут)
        List<PathDto> fastPath = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(8, 0))
                        .transport(Transport.BUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(8, 15))
                        .transport(Transport.BUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build()
        );

        List<List<PathDto>> paths = List.of(slowPath, fastPath);
        when(raptor.planAllPaths(originId, targetId, departureTime, day)).thenReturn(paths);

        List<RoutingResponseDto> results = routingService.findAllPaths(request);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).routeTime()).isEqualTo("00:15");
        assertThat(results.get(1).routeTime()).isEqualTo("01:00");
    }

    @Test
    void findAllPaths_shouldHandleMidnightCrossing_whenSorting() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(23, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<PathDto> midnightPath = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(23, 30))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(0, 15))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build()
        );

        List<PathDto> normalPath = List.of(
                PathDto.builder()
                        .stop(stop1)
                        .time(LocalTime.of(23, 0))
                        .transport(Transport.BUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build(),
                PathDto.builder()
                        .stop(stop2)
                        .time(LocalTime.of(23, 30))
                        .transport(Transport.BUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build()
        );

        List<List<PathDto>> paths = List.of(midnightPath, normalPath);
        when(raptor.planAllPaths(originId, targetId, departureTime, day)).thenReturn(paths);

        List<RoutingResponseDto> results = routingService.findAllPaths(request);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).routeTime()).isEqualTo("00:30");
        assertThat(results.get(1).routeTime()).isEqualTo("00:45");
    }

    @Test
    void findAllPaths_shouldLimitResults_toTop5() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<List<PathDto>> paths = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int minutes = 15 + i * 5;
            int hour = 8 + (minutes / 60);
            int minute = minutes % 60;
            paths.add(List.of(
                    PathDto.builder()
                            .stop(stop1)
                            .time(LocalTime.of(8, 0))
                            .transport(Transport.BUS)
                            .number(i)
                            .directionName("Direction " + i)
                            .build(),

                    PathDto.builder()
                            .stop(stop2)
                            .time(LocalTime.of(hour, minute))
                            .transport(Transport.BUS)
                            .number(i)
                            .directionName("Direction " + i)
                            .build()
            ));
        }

        when(raptor.planAllPaths(originId, targetId, departureTime, day)).thenReturn(paths);

        List<RoutingResponseDto> results = routingService.findAllPaths(request);

        assertThat(results).hasSize(5);

        assertThat(results.get(0).routeTime()).isEqualTo("00:15");
        assertThat(results.get(1).routeTime()).isEqualTo("00:20");
        assertThat(results.get(2).routeTime()).isEqualTo("00:25");
        assertThat(results.get(3).routeTime()).isEqualTo("00:30");
        assertThat(results.get(4).routeTime()).isEqualTo("00:35");
    }

    @Test
    void findPath_shouldHandleSameStopInPath_whenCalculatingTotalStops() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stopA = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Description A")
                .build();

        Stop stopB = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Description B")
                .build();

        List<PathDto> path = List.of(
                PathDto.builder()
                        .stop(stopA)
                        .time(LocalTime.of(8, 0))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stopB)
                        .time(LocalTime.of(8, 15))
                        .transport(Transport.BUS)
                        .number(5)
                        .directionName("Direction 1")
                        .build(),
                PathDto.builder()
                        .stop(stopA)
                        .time(LocalTime.of(8, 30))
                        .transport(Transport.TROLLEYBUS)
                        .number(10)
                        .directionName("Direction 2")
                        .build()
        );

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(path);

        RoutingResponseDto result = routingService.findPath(request);

        assertThat(result.totalStops()).isEqualTo(2);
    }

    @Test
    void buildSegments_shouldHandleComplexTransfers() {
        UUID originId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        LocalTime departureTime = LocalTime.of(8, 0);
        Day day = Day.WEEKDAY;

        RoutingRequestDto request = new RoutingRequestDto(originId, targetId, departureTime, day);

        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop 1")
                .description("Desc 1")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop 2")
                .description("Desc 2")
                .build();

        Stop stop3 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop 3")
                .description("Desc 3")
                .build();

        Stop stop4 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop 4")
                .description("Desc 4")
                .build();

        Stop stop5 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop 5")
                .description("Desc 5")
                .build();

        List<PathDto> path = List.of(
                PathDto.builder().stop(stop1).time(LocalTime.of(8, 0))
                        .transport(Transport.BUS).number(5).directionName("Dir 1").build(),
                PathDto.builder().stop(stop2).time(LocalTime.of(8, 10))
                        .transport(Transport.BUS).number(5).directionName("Dir 1").build(),
                PathDto.builder().stop(stop3).time(LocalTime.of(8, 20))
                        .transport(Transport.BUS).number(5).directionName("Dir 1").build(),
                PathDto.builder().stop(stop3).time(LocalTime.of(8, 25))
                        .transport(Transport.TROLLEYBUS).number(10).directionName("Dir 2").build(),
                PathDto.builder().stop(stop4).time(LocalTime.of(8, 35))
                        .transport(Transport.TROLLEYBUS).number(10).directionName("Dir 2").build(),
                PathDto.builder().stop(stop4).time(LocalTime.of(8, 40))
                        .transport(Transport.BUS).number(15).directionName("Dir 3").build(),
                PathDto.builder().stop(stop5).time(LocalTime.of(8, 50))
                        .transport(Transport.BUS).number(15).directionName("Dir 3").build()
        );

        when(raptor.plan(originId, targetId, departureTime, day)).thenReturn(path);

        RoutingResponseDto result = routingService.findPath(request);

        assertThat(result.segments()).hasSize(3);
        assertThat(result.transfers()).isEqualTo(2);

        assertThat(result.segments().get(0).routeNumber()).isEqualTo(5);
        assertThat(result.segments().get(0).stopsCount()).isEqualTo(3);

        assertThat(result.segments().get(1).routeNumber()).isEqualTo(10);
        assertThat(result.segments().get(1).stopsCount()).isEqualTo(2);

        assertThat(result.segments().get(2).routeNumber()).isEqualTo(15);
        assertThat(result.segments().get(2).stopsCount()).isEqualTo(2);
    }
}