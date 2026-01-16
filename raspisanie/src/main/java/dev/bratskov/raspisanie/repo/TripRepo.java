package dev.bratskov.raspisanie.repo;

import dev.bratskov.raspisanie.mapper.Parser;
import dev.bratskov.raspisanie.model.Trip;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class TripRepo {
    private final List<Trip> trips;

    public TripRepo(Parser parser) {
        this.trips = parser.getTrips();
    }
}
