package dev.bratskov.raspisanie.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RouteControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllRoutes_shouldReturnNonEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    void getAllRoutes_shouldReturnOnlySpecifiedTransportType() throws Exception {
        mockMvc.perform(get("/api/v1/routes")
                        .param("type", "BUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].transport", everyItem(is("BUS"))));
    }


    @Test
    void getRouteById_shouldReturnRoute_whenExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/routes"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String routeId = JsonPath.read(response, "$[0].id");

        mockMvc.perform(get("/api/v1/routes/{id}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(routeId)));
    }


    @Test
    void getRouteById_shouldReturn404_whenRouteNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/routes/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRoutesByNumber_shouldReturnRoutes_whenNumberExists() throws Exception {
        String response = mockMvc.perform(get("/api/v1/routes"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer number = JsonPath.read(response, "$[0].number");

        mockMvc.perform(get("/api/v1/routes/number/{number}", number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[*].number", everyItem(is(number))));
    }

    @Test
    void getRoutesByNumber_shouldReturnEmptyList_whenNumberNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/routes/number/{number}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getStopsByRoute_shouldReturnStops_whenRouteAndTransportExist() throws Exception {
        String response = mockMvc.perform(get("/api/v1/routes"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer number = JsonPath.read(response, "$[0].number");
        String transport = JsonPath.read(response, "$[0].transport");

        mockMvc.perform(get("/api/v1/routes/number/{number}/stops", number)
                        .param("type", transport))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0]").isArray());
    }

    @Test
    void getStopsByRoute_shouldReturnEmptyList_whenRouteNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/routes/number/{number}/stops", 999999)
                        .param("type", "BUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}