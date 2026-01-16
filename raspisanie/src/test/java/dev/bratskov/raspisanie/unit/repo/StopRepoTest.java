package dev.bratskov.raspisanie.unit.repo;

import dev.bratskov.raspisanie.mapper.Parser;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.repo.StopRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopRepoTest {

    @Mock
    private Parser parser;

    private StopRepo stopRepo;

    @Test
    void constructor_shouldInitializeStopsFromParser() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Stop stop1 = Stop.builder().id(id1).name("Stop 1").build();
        Stop stop2 = Stop.builder().id(id2).name("Stop 2").build();

        Map<String, Stop> stopMap = new HashMap<>();
        stopMap.put("s1", stop1);
        stopMap.put("s2", stop2);

        when(parser.getStopMap()).thenReturn(stopMap);

        stopRepo = new StopRepo(parser);

        assertThat(stopRepo.getStopsMap()).hasSize(2).containsValues(stop1, stop2);
        assertThat(stopRepo.getStops()).hasSize(2).contains(stop1, stop2);
    }

    @Test
    void getStopById_shouldReturnStop_whenExists() {
        UUID targetId = UUID.randomUUID();
        Stop targetStop = Stop.builder().id(targetId).name("Target Stop").build();
        Stop otherStop = Stop.builder().id(UUID.randomUUID()).name("Other Stop").build();

        Map<String, Stop> stopMap = Map.of("key1", targetStop, "key2", otherStop);
        when(parser.getStopMap()).thenReturn(stopMap);

        stopRepo = new StopRepo(parser);

        Stop result = stopRepo.getStopById(targetId);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(targetStop);
        assertThat(result.name()).isEqualTo("Target Stop");
    }

    @Test
    void getStopById_shouldReturnNull_whenNotFound() {
        Stop stop = Stop.builder().id(UUID.randomUUID()).name("Existing Stop").build();

        when(parser.getStopMap()).thenReturn(Map.of("key", stop));
        stopRepo = new StopRepo(parser);

        Stop result = stopRepo.getStopById(UUID.randomUUID());

        assertThat(result).isNull();
    }
}