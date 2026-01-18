package dev.bratskov.raspisanie.e2e.scenario;

import dev.bratskov.raspisanie.dto.ScheduleResponseDto;
import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.e2e.support.StopTestSupport;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Day;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CheckScheduleTest {
    @Autowired
    private TestRestTemplate testTemplate;

    private StopTestSupport stopTestSupport;

    @BeforeEach
    void setup() {
        stopTestSupport = new StopTestSupport(testTemplate);
    }

    @Test
    void userCanCheckScheduleForStopByRoute() {
        ResponseEntity<ShortRouteDto[]> response = testTemplate.getForEntity(
                "/api/v1/routes",
                ShortRouteDto[].class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ShortRouteDto[] routes = response.getBody();
        assertThat(routes).isNotEmpty();

        assertNotNull(routes);

        ShortRouteDto chosenRoute = Arrays.stream(routes)
                .filter(route -> route.number() == 8) // const!
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Route not found "));


        ResponseEntity<Stop[][]> stopsResponse = testTemplate.getForEntity(
                "/api/v1/routes/number/{number}/stops?type={type}",
                Stop[][].class,
                chosenRoute.number(),
                chosenRoute.transport()
        );
        Stop[][] stops = stopsResponse.getBody();
        assertThat(stopsResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(stops).isNotEmpty();

        assertNotNull(stops);
        Stop chosenStop = stops[0][0];

        ResponseEntity<ScheduleResponseDto[]> scheduleResponse = testTemplate.getForEntity(
                "/api/v1/stops/{stopId}/schedule/route/{routeId}?day={day}",
                ScheduleResponseDto[].class,
                chosenStop.id(),
                chosenRoute.id(),
                Day.WEEKDAY
        );
        assertThat(scheduleResponse.getStatusCode().is2xxSuccessful()).isTrue();

        ScheduleResponseDto[] schedules = scheduleResponse.getBody();
        assertThat(schedules).isNotEmpty();
        assertNotNull(schedules);
    }


    @Test
    void userCanCheckScheduleForStopByAllRoutes() {
        UUID stopId = stopTestSupport.findStopIdByName("ПЛОЩАДЬ СОВЕТСКАЯ",
                "ПЛОЩАДЬ СОВЕТСКАЯ _ОБЛАСТНАЯ ФИЛАРМОНИЯ");

        ResponseEntity<ScheduleResponseDto[]> scheduleResponse = testTemplate.getForEntity(
                "/api/v1/stops/{stopId}/schedule?day={day}",
                ScheduleResponseDto[].class,
                stopId,
                Day.WEEKDAY
        );

        assertThat(scheduleResponse.getStatusCode().is2xxSuccessful()).isTrue();

        ScheduleResponseDto[] schedules = scheduleResponse.getBody();
        assertThat(schedules).isNotEmpty();
        assertNotNull(schedules);
    }
}
