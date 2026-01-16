package dev.bratskov.raspisanie.unit.mapper;

import dev.bratskov.raspisanie.dto.ShortRouteDto;
import dev.bratskov.raspisanie.mapper.RouteMapper;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.enums.Transport;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RouteMapperTest {

    private final RouteMapper mapper = new RouteMapper();

    @Test
    void toShortRouteDto_shouldMapAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        Route route = Route.builder()
                .id(id)
                .number(42)
                .direction("Центр - Окраина")
                .transport(Transport.BUS)
                .build();

        ShortRouteDto result = mapper.toShortRouteDto(route);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.number()).isEqualTo(42);
        assertThat(result.direction()).isEqualTo("Центр - Окраина");
        assertThat(result.transport()).isEqualTo(Transport.BUS);
    }

    @Test
    void toShortRouteDto_shouldHandleNullFields() {
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .number(1)
                .direction(null)
                .transport(null)
                .build();

        ShortRouteDto result = mapper.toShortRouteDto(route);

        assertThat(result.direction()).isNull();
        assertThat(result.transport()).isNull();
        assertThat(result.number()).isEqualTo(1);
    }
}