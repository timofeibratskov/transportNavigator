package dev.bratskov.raspisanie.unit.service;

import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.exception.ResourceNotFoundException;
import dev.bratskov.raspisanie.mapper.RouteMapper;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.repo.RouteRepo;
import dev.bratskov.raspisanie.service.RouteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepo routeRepo;

    @Mock
    private RouteMapper routeMapper;

    @InjectMocks
    private RouteService routeService;

    @Test
    void findAll_shouldReturnAllRoutes_whenTypeIsNull() {
        Route route1 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction 1")
                .stops(List.of())
                .build();

        Route route2 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.TROLLEYBUS)
                .number(10)
                .direction("Direction 2")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(route1, route2));
        when(routeRepo.getRoutes()).thenReturn(routes);

        ShortRouteDto dto1 = new ShortRouteDto(route1.id(), 5, "Direction 1", Transport.BUS);
        ShortRouteDto dto2 = new ShortRouteDto(route2.id(), 10, "Direction 2", Transport.TROLLEYBUS);

        when(routeMapper.toShortRouteDto(route1)).thenReturn(dto1);
        when(routeMapper.toShortRouteDto(route2)).thenReturn(dto2);

        List<ShortRouteDto> result = routeService.findAll(null);

        assertThat(result).hasSize(2);
        verify(routeMapper, times(2)).toShortRouteDto(any(Route.class));
    }

    @Test
    void findAll_shouldReturnOnlyBusRoutes_whenTypeIsBus() {
        Route busRoute = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Bus Direction")
                .stops(List.of())
                .build();

        Route trolleybusRoute = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.TROLLEYBUS)
                .number(10)
                .direction("Trolleybus Direction")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(busRoute, trolleybusRoute));
        when(routeRepo.getRoutes()).thenReturn(routes);

        ShortRouteDto busDto = new ShortRouteDto(busRoute.id(), 5, "Bus Direction", Transport.BUS);
        when(routeMapper.toShortRouteDto(busRoute)).thenReturn(busDto);

        List<ShortRouteDto> result = routeService.findAll(Transport.BUS);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().transport()).isEqualTo(Transport.BUS);
        verify(routeMapper, times(1)).toShortRouteDto(busRoute);
        verify(routeMapper, never()).toShortRouteDto(trolleybusRoute);
    }

    @Test
    void findAll_shouldReturnSortedByNumber() {
        Route route1 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(10)
                .direction("Direction 1")
                .stops(List.of())
                .build();

        Route route2 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction 2")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(route1, route2));
        when(routeRepo.getRoutes()).thenReturn(routes);

        ShortRouteDto dto1 = new ShortRouteDto(route1.id(), 10, "Direction 1", Transport.BUS);
        ShortRouteDto dto2 = new ShortRouteDto(route2.id(), 5, "Direction 2", Transport.BUS);

        when(routeMapper.toShortRouteDto(route1)).thenReturn(dto1);
        when(routeMapper.toShortRouteDto(route2)).thenReturn(dto2);

        List<ShortRouteDto> result = routeService.findAll(null);

        assertThat(result).extracting(ShortRouteDto::number).containsExactly(5, 10);
    }

    @Test
    void findById_shouldReturnRoute_whenRouteExists() {
        UUID routeId = UUID.randomUUID();
        Route route = Route.builder()
                .id(routeId)
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Collections.singletonList(route));
        when(routeRepo.getRoutes()).thenReturn(routes);

        Route result = routeService.findById(routeId);

        assertThat(result).isEqualTo(route);
    }

    @Test
    void findById_shouldThrowException_whenRouteDoesNotExist() {
        UUID nonExistentId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();

        Route route = Route.builder()
                .id(existingId)
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Collections.singletonList(route));
        when(routeRepo.getRoutes()).thenReturn(routes);

        assertThatThrownBy(() -> routeService.findById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Маршрут с id=" + nonExistentId + " не найден");
    }

    @Test
    void findByNumber_shouldReturnAllRoutesWithNumber() {
        Route route1 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction 1")
                .stops(List.of())
                .build();

        Route route2 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.TROLLEYBUS)
                .number(5)
                .direction("Direction 2")
                .stops(List.of())
                .build();

        Route route3 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(10)
                .direction("Direction 3")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(route1, route2, route3));
        when(routeRepo.getRoutes()).thenReturn(routes);

        List<Route> result = routeService.findByNumber(5);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(route1, route2);
    }

    @Test
    void findByNumber_shouldReturnEmptyList_whenNoRoutesWithNumber() {
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Collections.singletonList(route));
        when(routeRepo.getRoutes()).thenReturn(routes);

        List<Route> result = routeService.findByNumber(999);

        assertThat(result).isEmpty();
    }

    @Test
    void findByNumberAndType_shouldReturnFilteredRoutes_whenTypeIsNotNull() {
        Route busRoute = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Bus Direction")
                .stops(List.of())
                .build();

        Route trolleybusRoute = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.TROLLEYBUS)
                .number(5)
                .direction("Trolleybus Direction")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(busRoute, trolleybusRoute));
        when(routeRepo.getRoutes()).thenReturn(routes);

        List<Route> result = routeService.findByNumber(5, Transport.BUS);

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(busRoute);
    }

    @Test
    void findByNumberAndType_shouldReturnAllRoutesWithNumber_whenTypeIsNull() {
        Route route1 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction 1")
                .stops(List.of())
                .build();

        Route route2 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.TROLLEYBUS)
                .number(5)
                .direction("Direction 2")
                .stops(List.of())
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(route1, route2));
        when(routeRepo.getRoutes()).thenReturn(routes);

        List<Route> result = routeService.findByNumber(5, null);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(route1, route2);
    }

    @Test
    void findByStopId_shouldReturnRoutesContainingStop() {
        UUID stopId = UUID.randomUUID();

        Stop stop1 = Stop.builder()
                .id(stopId)
                .name("Stop 1")
                .description("Description 1")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop 2")
                .description("Description 2")
                .build();

        Route route1 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction 1")
                .stops(List.of(stop1, stop2))
                .build();

        Route route2 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(10)
                .direction("Direction 2")
                .stops(List.of(stop2))
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(route1, route2));
        when(routeRepo.getRoutes()).thenReturn(routes);

        ShortRouteDto dto1 = new ShortRouteDto(route1.id(), 5, "Direction 1", Transport.BUS);
        when(routeMapper.toShortRouteDto(route1)).thenReturn(dto1);

        List<ShortRouteDto> result = routeService.findByStopId(stopId);

        assertThat(result).hasSize(1);
        verify(routeMapper, times(1)).toShortRouteDto(route1);
        verify(routeMapper, never()).toShortRouteDto(route2);
    }

    @Test
    void findByStopId_shouldReturnEmptyList_whenNoRoutesContainStop() {
        UUID stopId = UUID.randomUUID();

        Stop stop = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop")
                .description("Description")
                .build();

        Route route = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction")
                .stops(List.of(stop))
                .build();

        Set<Route> routes = new HashSet<>(Collections.singletonList(route));
        when(routeRepo.getRoutes()).thenReturn(routes);

        List<ShortRouteDto> result = routeService.findByStopId(stopId);

        assertThat(result).isEmpty();
        verify(routeMapper, never()).toShortRouteDto(any(Route.class));
    }

    @Test
    void findByStopId_shouldReturnSortedByNumber() {
        UUID stopId = UUID.randomUUID();

        Stop stop = Stop.builder()
                .id(stopId)
                .name("Stop")
                .description("Description")
                .build();

        Route route1 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(10)
                .direction("Direction 1")
                .stops(List.of(stop))
                .build();

        Route route2 = Route.builder()
                .id(UUID.randomUUID())
                .transport(Transport.BUS)
                .number(5)
                .direction("Direction 2")
                .stops(List.of(stop))
                .build();

        Set<Route> routes = new HashSet<>(Arrays.asList(route1, route2));
        when(routeRepo.getRoutes()).thenReturn(routes);

        ShortRouteDto dto1 = new ShortRouteDto(route1.id(), 10, "Direction 1", Transport.BUS);
        ShortRouteDto dto2 = new ShortRouteDto(route2.id(), 5, "Direction 2", Transport.BUS);

        when(routeMapper.toShortRouteDto(route1)).thenReturn(dto1);
        when(routeMapper.toShortRouteDto(route2)).thenReturn(dto2);

        List<ShortRouteDto> result = routeService.findByStopId(stopId);

        assertThat(result).extracting(ShortRouteDto::number).containsExactly(5, 10);
    }
}