package dev.bratskov.raspisanie.e2e.scenario;

import dev.bratskov.raspisanie.dto.RoutingRequestDto;
import dev.bratskov.raspisanie.dto.RoutingResponseDto;
import dev.bratskov.raspisanie.e2e.support.StopTestSupport;
import dev.bratskov.raspisanie.model.enums.Day;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class BuildRouteTest {

    @Autowired
    private TestRestTemplate testTemplate;

    private StopTestSupport stopTestSupport;

    @BeforeEach
    void setup() {
        stopTestSupport = new StopTestSupport(testTemplate);
    }

    @Test
    void userCanBuildRouteBetweenTwoStops() {
        UUID originStopId = stopTestSupport.findStopIdByName("улица буденного", "УЛИЦА БУДЕННОГО _ЗООПАРК");
        UUID targetStopId = stopTestSupport.findStopIdByName("УНИВЕРСИТЕТ", "УНИВЕРСИТЕТ _УЛИЦА ЛЕНИНА");

        RoutingRequestDto requestDto = RoutingRequestDto.builder()
                .originStopId(originStopId)
                .targetStopId(targetStopId)
                .time(LocalTime.of(11, 54))
                .day(Day.WEEKDAY)
                .build();

        ResponseEntity<RoutingResponseDto> response = testTemplate.postForEntity(
                "/api/v1/routing/plan",
                requestDto,
                RoutingResponseDto.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        RoutingResponseDto responseDto = response.getBody();
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.totalStops()).isGreaterThan(0);
        assertThat(responseDto.fullPath()).isNotEmpty();
    }

    @Test
    void userCanBuildAllRoutesBetweenTwoStops() {
        UUID originStopId = stopTestSupport.findStopIdByName("улица буденного", "УЛИЦА БУДЕННОГО _ЗООПАРК");
        UUID targetStopId = stopTestSupport.findStopIdByName("УНИВЕРСИТЕТ", "УНИВЕРСИТЕТ _УЛИЦА ЛЕНИНА");

        RoutingRequestDto requestDto = RoutingRequestDto.builder()
                .originStopId(originStopId)
                .targetStopId(targetStopId)
                .time(LocalTime.of(11, 54))
                .day(Day.WEEKDAY)
                .build();

        ResponseEntity<RoutingResponseDto[]> response = testTemplate.exchange(
                "/api/v1/routing/plan/all",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(requestDto),
                RoutingResponseDto[].class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        RoutingResponseDto[] responseArray = response.getBody();
        assertThat(responseArray).isNotNull();
        assertThat(responseArray).hasSizeGreaterThan(0);

        List<RoutingResponseDto> responseList = Arrays.asList(responseArray);
        RoutingResponseDto firstRoute = responseList.getFirst();
        assertThat(firstRoute.totalStops()).isGreaterThan(0);
        assertThat(firstRoute.fullPath()).isNotEmpty();
    }


}
