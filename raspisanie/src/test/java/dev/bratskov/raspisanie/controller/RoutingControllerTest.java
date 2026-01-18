
package dev.bratskov.raspisanie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bratskov.raspisanie.dto.PathDto;
import dev.bratskov.raspisanie.dto.RoutingRequestDto;
import dev.bratskov.raspisanie.dto.RoutingResponseDto;
import dev.bratskov.raspisanie.dto.SegmentDto;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(RoutingController.class)
class RoutingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoutingService routingService;

    @Test
    void getPlan_shouldReturnRoute_whenValidRequest() throws Exception {
        RoutingRequestDto request = new RoutingRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalTime.of(8, 30),
                Day.WEEKDAY
        );

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

        RoutingResponseDto response = RoutingResponseDto.builder()
                .routeTime("00:30")
                .totalStops(2)
                .transfers(0)
                .fullPath(List.of(
                        PathDto.builder()
                                .stop(stop1)
                                .time(LocalTime.of(8, 30))
                                .transport(Transport.BUS)
                                .number(5)
                                .directionName("A - B")
                                .build(),
                        PathDto.builder()
                                .stop(stop2)
                                .time(LocalTime.of(9, 0))
                                .transport(Transport.BUS)
                                .number(5)
                                .directionName("A - B")
                                .build()
                ))
                .segments(List.of(
                        SegmentDto.builder()
                                .boardingStop(stop1)
                                .exitStop(stop2)
                                .routeNumber(5)
                                .stopsCount(2)
                                .build()
                ))
                .build();

        when(routingService.findPath(any(RoutingRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeTime").value("00:30"))
                .andExpect(jsonPath("$.totalStops").value(2))
                .andExpect(jsonPath("$.transfers").value(0));
    }

    @Test
    void getPlan_shouldReturn400_whenOriginIdIsNull() throws Exception {
        RoutingRequestDto request = new RoutingRequestDto(
                null,
                UUID.randomUUID(),
                LocalTime.of(8, 30),
                Day.WEEKDAY
        );

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.originStopId").exists());
    }

    @Test
    void getPlan_shouldReturn400_whenTimeIsNull() throws Exception {
        RoutingRequestDto request = new RoutingRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                Day.WEEKDAY
        );

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getPlan_shouldReturn404_whenNoRouteFound() throws Exception {
        RoutingRequestDto request = new RoutingRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalTime.of(8, 30),
                Day.WEEKDAY
        );

        when(routingService.findPath(any(RoutingRequestDto.class)))
                .thenThrow(new NoSuchElementException("Маршрут не найден"));

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Маршрут не найден"));
    }

    @Test
    void getAllPlans_shouldReturnMultipleRoutes() throws Exception {
        RoutingRequestDto request = new RoutingRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalTime.of(8, 30),
                Day.WEEKDAY
        );


        RoutingResponseDto route1 = RoutingResponseDto.builder()
                .routeTime("00:15")
                .totalStops(2)
                .transfers(0)
                .fullPath(List.of())
                .segments(List.of())
                .build();

        RoutingResponseDto route2 = RoutingResponseDto.builder()
                .routeTime("00:30")
                .totalStops(3)
                .transfers(1)
                .fullPath(List.of())
                .segments(List.of())
                .build();

        when(routingService.findAllPaths(any(RoutingRequestDto.class)))
                .thenReturn(List.of(route1, route2));

        mockMvc.perform(post("/api/v1/routing/plan/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].routeTime").value("00:15"))
                .andExpect(jsonPath("$[1].routeTime").value("00:30"));
    }

    @Test
    void getPlan_shouldReturn400_whenSameOriginAndTarget() throws Exception {
        UUID sameId = UUID.randomUUID();
        RoutingRequestDto request = new RoutingRequestDto(
                sameId,
                sameId,
                LocalTime.of(8, 30),
                Day.WEEKDAY
        );

        when(routingService.findPath(any(RoutingRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Начальная и конечная остановки совпадают!"));

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Начальная и конечная остановки совпадают!"));
    }
}