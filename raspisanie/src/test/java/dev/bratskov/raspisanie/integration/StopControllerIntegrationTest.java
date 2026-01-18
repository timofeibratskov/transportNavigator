package dev.bratskov.raspisanie.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StopControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllStops_shouldReturnNonEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void getStopById_shouldReturn404_whenNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/stops/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStopById_shouldReturnStop_whenExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String firstStopId = JsonPath.read(response, "$[0].id");

        mockMvc.perform(get("/api/v1/stops/" + firstStopId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstStopId)))
                .andExpect(jsonPath("$.name", not(emptyOrNullString())))
                .andExpect(jsonPath("$.description", not(emptyOrNullString())));
    }


    @Test
    void getAllRoutesByStop_shouldReturnRoutes_whenExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(response, "$[0].id");

        mockMvc.perform(get("/api/v1/stops/{id}/routes", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void getAllRoutesByStop_shouldReturnEmptyList_whenStopNotExists() throws Exception {

        mockMvc.perform(get("/api/v1/stops/{id}/routes", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllRoutesScheduleByStop_shouldReturn404_whenStopNotExists() throws Exception {

        mockMvc.perform(get("/api/v1/stops/{id}/schedule", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllRoutesScheduleByStop_shouldReturnSchedule_whenStopExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(response, "$[0].id");

        mockMvc.perform(get("/api/v1/stops/{id}/schedule", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void getAllRoutesScheduleByStop_shouldReturnSchedule_whenDayAndStopExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = JsonPath.read(response, "$[0].id");

        mockMvc.perform(get("/api/v1/stops/{id}/schedule", id)
                        .param("day", "WEEKDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[*].day", everyItem(is("WEEKDAY"))));

    }

    @Test
    void getSpecificRouteScheduleByStop_shouldReturnSchedule_whenDayAndStopAndRouteExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> ids = JsonPath.read(response,
                "$[?(@.name=='ПЛОЩАДЬ СОВЕТСКАЯ' && @.description=='ПЛОЩАДЬ СОВЕТСКАЯ _ДОМ СВЯЗИ')].id");
        String stopId = ids.getFirst();

        String response2 = mockMvc.perform(get("/api/v1/stops/{id}/routes", stopId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String routeId = JsonPath.read(response2, "$[0].id");

        mockMvc.perform(get("/api/v1/stops/{stopId}/schedule/route/{routeId}", stopId, routeId)
                        .param("day", "WEEKDAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[*].day", everyItem(is("WEEKDAY"))));
    }

    @Test
    void getSpecificRouteScheduleByStop_shouldReturn404_whenOnlyRouteNotExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String stopId = JsonPath.read(response, "$[0].id");

        mockMvc.perform(get("/api/v1/stops/{stopId}/schedule/route/{routeId}", stopId, UUID.randomUUID())
                        .param("day", "WEEKDAY"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSpecificRouteScheduleByStop_shouldReturnSchedule_whenOnlyStopNotExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/stops"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> ids = JsonPath.read(response,
                "$[?(@.name=='ПЛОЩАДЬ СОВЕТСКАЯ' && @.description=='ПЛОЩАДЬ СОВЕТСКАЯ _ДОМ СВЯЗИ')].id");
        String stopId = ids.getFirst();

        String response2 = mockMvc.perform(get("/api/v1/stops/{id}/routes", stopId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String routeId = JsonPath.read(response2, "$[0].id");
        stopId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/stops/{stopId}/schedule/route/{routeId}", stopId, routeId)
                        .param("day", "WEEKDAY"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchStops_shouldReturnMatchingStops_whenNameExists() throws Exception {
        mockMvc.perform(get("/api/v1/stops/search")
                        .param("name", "советская"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[*].name",
                        hasItem(containsString("ПЛОЩАДЬ СОВЕТСКАЯ"))));
    }

    @Test
    void searchStops_shouldReturnMatchingStops_whenNameWithMarkExists() throws Exception {
        mockMvc.perform(get("/api/v1/stops/search")
                        .param("name", "крокодил"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void searchStops_shouldReturnStop_whenNameIsMisspelled() throws Exception {
        mockMvc.perform(get("/api/v1/stops/search")
                        .param("name", "площадь светска"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[*].name",
                        hasItem(containsString("ПЛОЩАДЬ СОВЕТСКАЯ"))));
    }

    @Test
    void searchStops_shouldReturnStop_whenFirstCharNameIsEquals() throws Exception {
        mockMvc.perform(get("/api/v1/stops/search")
                        .param("name", "у"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[*].name",
                        hasItem(containsString("УЛИЦА"))));
    }
}
