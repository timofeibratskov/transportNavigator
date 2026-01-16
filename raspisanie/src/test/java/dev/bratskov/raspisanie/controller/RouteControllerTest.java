package dev.bratskov.raspisanie.controller;

import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.exception.ResourceNotFoundException;
import dev.bratskov.raspisanie.mapper.RouteMapper;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.service.RouteService;
import dev.bratskov.raspisanie.service.StopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private StopService stopService;

    @MockitoBean
    private RouteMapper routeMapper;

    @Test
    void getAllRoutes_shouldReturnAllRoutes_whenNoTypeProvided() throws Exception {
        List<ShortRouteDto> routes = List.of(
                new ShortRouteDto(UUID.randomUUID(), 5, "A - B", Transport.BUS),
                new ShortRouteDto(UUID.randomUUID(), 10, "X - Y", Transport.TROLLEYBUS)
        );

        when(routeService.findAll(null)).thenReturn(routes);

        mockMvc.perform(get("/api/v1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value(5))
                .andExpect(jsonPath("$[1].number").value(10));
    }

    @Test
    void getAllRoutes_shouldReturnFilteredRoutes_whenTypeProvided() throws Exception {
        List<ShortRouteDto> routes = List.of(
                new ShortRouteDto(UUID.randomUUID(), 5, "A - B", Transport.BUS)
        );

        when(routeService.findAll(Transport.BUS)).thenReturn(routes);

        mockMvc.perform(get("/api/v1/routes")
                        .param("type", "BUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transport").value("BUS"));
    }

    @Test
    void getRouteById_shouldReturnRoute_whenExists() throws Exception {
        UUID routeId = UUID.randomUUID();
        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Desc A")
                .build();

        Route route = Route.builder()
                .id(routeId)
                .number(5)
                .direction("A - B")
                .transport(Transport.BUS)
                .stops(List.of(stop1))
                .build();

        when(routeService.findById(routeId)).thenReturn(route);

        mockMvc.perform(get("/api/v1/routes/{id}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(routeId.toString()))
                .andExpect(jsonPath("$.number").value(5))
                .andExpect(jsonPath("$.direction").value("A - B"));
    }

    @Test
    void getRouteById_shouldReturn404_whenNotExists() throws Exception {
        UUID routeId = UUID.randomUUID();

        when(routeService.findById(routeId))
                .thenThrow(new ResourceNotFoundException("Маршрут не найден"));

        mockMvc.perform(get("/api/v1/routes/{id}", routeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Маршрут не найден"));
    }

    @Test
    void getRoutesByNumber_shouldReturnRoutes_whenNoTypeProvided() throws Exception {
        Route route1 = Route.builder()
                .id(UUID.randomUUID())
                .number(5)
                .direction("A - B")
                .transport(Transport.BUS)
                .stops(List.of())
                .build();

        Route route2 = Route.builder()
                .id(UUID.randomUUID())
                .number(5)
                .direction("B - A")
                .transport(Transport.BUS)
                .stops(List.of())
                .build();

        ShortRouteDto dto1 = new ShortRouteDto(route1.id(), 5, "A - B", Transport.BUS);
        ShortRouteDto dto2 = new ShortRouteDto(route2.id(), 5, "B - A", Transport.BUS);

        when(routeService.findByNumber(5, null)).thenReturn(List.of(route1, route2));
        when(routeMapper.toShortRouteDto(route1)).thenReturn(dto1);
        when(routeMapper.toShortRouteDto(route2)).thenReturn(dto2);

        mockMvc.perform(get("/api/v1/routes/number/{number}", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getRoutesByNumber_shouldReturnFilteredRoutes_whenTypeProvided() throws Exception {
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .number(5)
                .direction("A - B")
                .transport(Transport.BUS)
                .stops(List.of())
                .build();

        ShortRouteDto dto = new ShortRouteDto(route.id(), 5, "A - B", Transport.BUS);

        when(routeService.findByNumber(5, Transport.BUS)).thenReturn(List.of(route));
        when(routeMapper.toShortRouteDto(route)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/routes/number/{number}", 5)
                        .param("type", "BUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transport").value("BUS"));
    }

    @Test
    void getStopsByRoute_shouldReturnStops() throws Exception {
        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop A")
                .description("Desc A")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Stop B")
                .description("Desc B")
                .build();

        List<List<Stop>> stops = List.of(List.of(stop1, stop2));

        when(stopService.findByRouteIdAndTransportType(5, Transport.BUS)).thenReturn(stops);

        mockMvc.perform(get("/api/v1/routes/number/{number}/stops", 5)
                        .param("type", "BUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].length()").value(2));
    }

    @Test
    void getRouteById_shouldReturn400_whenInvalidUUID() throws Exception {
        mockMvc.perform(get("/api/v1/routes/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }
}