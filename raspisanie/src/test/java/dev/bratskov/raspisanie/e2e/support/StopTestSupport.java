package dev.bratskov.raspisanie.e2e.support;

import dev.bratskov.raspisanie.model.Stop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class StopTestSupport {

    TestRestTemplate testTemplate;

    public StopTestSupport(TestRestTemplate testTemplate) {
        this.testTemplate = testTemplate;
    }


    public UUID findStopIdByName(String name, String description) {
        ResponseEntity<Stop[]> response = testTemplate.getForEntity(
                "/api/v1/stops/search?name=" + name,
                Stop[].class
        );
        Stop[] stops = response.getBody();
        assertThat(stops).isNotNull();
        assertThat(stops).hasSizeGreaterThan(0);

        return Arrays.stream(stops)
                .filter(stop -> description.equals(stop.description()))
                .map(Stop::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Stop not found with name='" + name + "' and description='" + description + "'"
                ));
    }
}
