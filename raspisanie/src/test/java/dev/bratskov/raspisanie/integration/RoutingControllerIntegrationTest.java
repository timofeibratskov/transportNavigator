package dev.bratskov.raspisanie.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import dev.bratskov.raspisanie.dto.RoutingRequestDto;
import dev.bratskov.raspisanie.model.enums.Day;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RoutingControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllPlans_shouldReturnMultipleRoutes_whenAvailable() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> ids = JsonPath.read(response,
                "$[?(@.name=='ВИШНЕВЕЦ' && @.description=='ВИШНЕВЕЦ _КОНЕЧНАЯ')].id");
        String originStopId = ids.getFirst();

        ids = JsonPath.read(response,
                "$[?(@.name=='УНИВЕРСИТЕТ' && @.description=='УНИВЕРСИТЕТ _ТЕАТР КУКОЛ')].id");
        String targetStopId = ids.getFirst();

        RoutingRequestDto request = RoutingRequestDto.builder()
                .originStopId(UUID.fromString(originStopId))
                .targetStopId(UUID.fromString(targetStopId))
                .day(Day.WEEKDAY)
                .time(LocalTime.of(13, 30))
                .build();

        mockMvc.perform(post("/api/v1/routing/plan/all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].routeTime").exists());
    }

    @Test
    void getPlan_shouldReturn400_whenRouteNotFound() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> ids = JsonPath.read(response,
                "$[?(@.name=='ВИШНЕВЕЦ' && @.description=='ВИШНЕВЕЦ _КОНЕЧНАЯ')].id");
        String originStopId = ids.getFirst();

        RoutingRequestDto request = RoutingRequestDto.builder()
                .originStopId(UUID.fromString(originStopId))
                .targetStopId(UUID.fromString(originStopId))
                .day(Day.WEEKDAY)
                .time(LocalTime.of(13, 30))
                .build();

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPlan_shouldReturn404_whenRouteNotFound() throws Exception {
        RoutingRequestDto request = new RoutingRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalTime.of(3, 0),
                Day.WEEKDAY
        );

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }


    @Test
    void getPlan_shouldReturnRoute_whenDataExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> ids = JsonPath.read(response,
                "$[?(@.name=='ВИШНЕВЕЦ' && @.description=='ВИШНЕВЕЦ _КОНЕЧНАЯ')].id");
        String originStopId = ids.getFirst();

        ids = JsonPath.read(response,
                "$[?(@.name=='УНИВЕРСИТЕТ' && @.description=='УНИВЕРСИТЕТ _ТЕАТР КУКОЛ')].id");
        String targetStopId = ids.getFirst();

        RoutingRequestDto request = RoutingRequestDto.builder()
                .originStopId(UUID.fromString(originStopId))
                .targetStopId(UUID.fromString(targetStopId))
                .day(Day.WEEKDAY)
                .time(LocalTime.of(13, 30))
                .build();

        mockMvc.perform(post("/api/v1/routing/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeTime").exists())
                .andExpect(jsonPath("$.totalStops", greaterThan(0)))
                .andExpect(jsonPath("$.fullPath").isArray())
                .andExpect(jsonPath("$.fullPath").isNotEmpty())
                .andExpect(jsonPath("$.segments").isArray());
    }
}


