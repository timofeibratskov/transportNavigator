package dev.bratskov.raspisanie.repo;

import dev.bratskov.raspisanie.mapper.Parser;
import dev.bratskov.raspisanie.model.Stop;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Component
public class StopRepo {

    private final Set<Stop> stops;
    private final Map<String, Stop> stopsMap;

    public StopRepo(Parser parser) {
        this.stops = new HashSet<>(parser.getStopMap().values());
        this.stopsMap = parser.getStopMap();
    }

    public Stop getStopById(UUID id) {
        return stops.stream()
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}
