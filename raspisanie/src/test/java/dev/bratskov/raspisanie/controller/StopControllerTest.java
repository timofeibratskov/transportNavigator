package dev.bratskov.raspisanie.controller;

import dev.bratskov.raspisanie.dto.ScheduleResponseDto;
import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.exception.ResourceNotFoundException;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.service.RouteService;
import dev.bratskov.raspisanie.service.ScheduleService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StopController.class)
class StopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StopService stopService;

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private ScheduleService scheduleService;

    @Test
    void getStopById_shouldReturnStop_whenExists() throws Exception {
        UUID stopId = UUID.randomUUID();
        Stop stop = Stop.builder()
                .id(stopId)
                .name("Central Station")
                .description("Main station")
                .build();

        when(stopService.findById(stopId)).thenReturn(stop);

        mockMvc.perform(get("/api/v1/stops/{id}", stopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stopId.toString()))
                .andExpect(jsonPath("$.name").value("Central Station"));
    }

    @Test
    void getStopById_shouldReturn404_whenNotExists() throws Exception {
        UUID stopId = UUID.randomUUID();

        when(stopService.findById(stopId))
                .thenThrow(new ResourceNotFoundException("Остановка не найдена"));

        mockMvc.perform(get("/api/v1/stops/{id}", stopId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void searchStops_shouldReturnMatchingStops() throws Exception {
        Stop stop1 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Central Park")
                .description("Park stop")
                .build();

        Stop stop2 = Stop.builder()
                .id(UUID.randomUUID())
                .name("Central Station")
                .description("Station stop")
                .build();

        when(stopService.findByName("Central")).thenReturn(List.of(stop1, stop2));

        mockMvc.perform(get("/api/v1/stops/search")
                        .param("name", "Central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Central Park"))
                .andExpect(jsonPath("$[1].name").value("Central Station"));
    }

    @Test
    void getAllStops_shouldReturnAllStops() throws Exception {
        List<Stop> stops = List.of(
                Stop.builder().id(UUID.randomUUID()).name("Stop 1").description("D1").build(),
                Stop.builder().id(UUID.randomUUID()).name("Stop 2").description("D2").build()
        );

        when(stopService.findAll()).thenReturn(stops);

        mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllRoutesByStop_shouldReturnRoutes() throws Exception {
        UUID stopId = UUID.randomUUID();
        List<ShortRouteDto> routes = List.of(
                new ShortRouteDto(UUID.randomUUID(), 5, "A - B", Transport.BUS),
                new ShortRouteDto(UUID.randomUUID(), 10, "X - Y", Transport.TROLLEYBUS)
        );

        when(routeService.findByStopId(stopId)).thenReturn(routes);

        mockMvc.perform(get("/api/v1/stops/{id}/routes", stopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllRoutesScheduleByStop_shouldReturnSchedule_whenNoDayProvided() throws Exception {
        UUID stopId = UUID.randomUUID();
        List<ScheduleResponseDto> schedule = List.of(
                ScheduleResponseDto.builder().build()
        );

        when(scheduleService.findAllByStopId(stopId, null)).thenReturn(schedule);

        mockMvc.perform(get("/api/v1/stops/{id}/schedule", stopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getAllRoutesScheduleByStop_shouldReturnSchedule_whenDayProvided() throws Exception {
        UUID stopId = UUID.randomUUID();
        List<ScheduleResponseDto> schedule = List.of(
                ScheduleResponseDto.builder().build()
        );

        when(scheduleService.findAllByStopId(stopId, Day.WEEKDAY)).thenReturn(schedule);

        mockMvc.perform(get("/api/v1/stops/{id}/schedule", stopId)
                        .param("day", "WEEKDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getSpecificRouteScheduleByStop_shouldReturnSchedule() throws Exception {
        UUID stopId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        List<ScheduleResponseDto> schedule = List.of(
                ScheduleResponseDto.builder().build()
        );

        when(scheduleService.findByStopIdAndRouteId(stopId, routeId, Day.WEEKEND))
                .thenReturn(schedule);

        mockMvc.perform(get("/api/v1/stops/{stopId}/schedule/route/{routeId}", stopId, routeId)
                        .param("day", "WEEKEND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}