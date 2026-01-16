package dev.bratskov.raspisanie.repo;

import dev.bratskov.raspisanie.mapper.Parser;
import dev.bratskov.raspisanie.model.Route;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Getter
public class RouteRepo {
    private final Set<Route> routes;

    public RouteRepo(Parser parser) {
        this.routes = new HashSet<>(parser.getRouteMap().values());
    }
}
