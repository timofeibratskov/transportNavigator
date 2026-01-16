package dev.bratskov.raspisanie.unit.repo;

import dev.bratskov.raspisanie.mapper.Parser;
import dev.bratskov.raspisanie.model.Trip;
import dev.bratskov.raspisanie.repo.TripRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripRepoTest {

    @Mock
    private Parser parser;

    private TripRepo tripRepo;

    @Test
    void constructor_shouldInitializeTripsFromParser() {
        Trip trip1 = mock(Trip.class);
        Trip trip2 = mock(Trip.class);

        List<Trip> tripList = List.of(trip1, trip2);

        when(parser.getTrips()).thenReturn(tripList);

        tripRepo = new TripRepo(parser);

        assertThat(tripRepo.getTrips()).hasSize(2);
        assertThat(tripRepo.getTrips()).isEqualTo(tripList);
    }

    @Test
    void constructor_shouldHandleEmptyList() {
        when(parser.getTrips()).thenReturn(Collections.emptyList());

        tripRepo = new TripRepo(parser);

        assertThat(tripRepo.getTrips()).isEmpty();
    }
}