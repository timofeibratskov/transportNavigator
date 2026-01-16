package dev.bratskov.raspisanie.unit.repo;

import dev.bratskov.raspisanie.mapper.Parser;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.repo.RouteRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteRepoTest {

    @Mock
    private Parser parser;

    private RouteRepo routeRepo;

    @Test
    void constructor_shouldInitializeRoutesFromParser() {
        Route route1 = Route.builder().number(1).build();
        Route route2 = Route.builder().number(2).build();

        Map<String, Route> routeMap = Map.of(
                "r1", route1,
                "r2", route2
        );

        when(parser.getRouteMap()).thenReturn(routeMap);

        routeRepo = new RouteRepo(parser);

        assertThat(routeRepo.getRoutes()).hasSize(2);
        assertThat(routeRepo.getRoutes()).contains(route1, route2);
    }

    @Test
    void constructor_shouldHandleEmptyParserData() {
        when(parser.getRouteMap()).thenReturn(Map.of());

        routeRepo = new RouteRepo(parser);

        assertThat(routeRepo.getRoutes()).isEmpty();
    }
}