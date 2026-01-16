package dev.bratskov.raspisanie.unit.service;

import dev.bratskov.raspisanie.exception.ResourceNotFoundException;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.repo.StopRepo;
import dev.bratskov.raspisanie.service.RouteService;
import dev.bratskov.raspisanie.service.StopSearchService;
import dev.bratskov.raspisanie.service.StopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StopServiceTest {

    @Mock
    private StopRepo stopRepo;

    @Mock
    private RouteService routeService;

    @Mock
    private StopSearchService stopSearchService;

    @InjectMocks
    private StopService stopService;

    @Test
    void findAll_shouldReturnSortedStopsByName() {
        Stop stop1 = Stop.builder().name("ВИШНЕВЕЦ").build();
        Stop stop2 = Stop.builder().name("УНИВЕРСИТЕТ").build();

        when(stopRepo.getStops()).thenReturn(new HashSet<>(Arrays.asList(stop1, stop2)));

        List<Stop> result = stopService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("ВИШНЕВЕЦ");
        assertThat(result.get(1).name()).isEqualTo("УНИВЕРСИТЕТ");
    }

    @Test
    void findByName_shouldCallSearchEngine() {
        String query = "ТРИНИТИ";
        Stop foundStop = Stop.builder().name("ТОРГОВЫЙ ЦЕНТР \"ТРИНИТИ\"").build();

        when(stopSearchService.searchByName(query)).thenReturn(List.of(foundStop));

        List<Stop> result = stopService.findByName(query);

        assertThat(result).containsExactly(foundStop);
        verify(stopSearchService, times(1)).searchByName(query);
    }

    @Test
    void findById_shouldReturnStop_whenExists() {
        UUID id = UUID.randomUUID();
        Stop stop = Stop.builder().id(id).name("ЖК \"КОЛБАСИНО\"").build();

        when(stopRepo.getStopById(id)).thenReturn(stop);

        Stop result = stopService.findById(id);

        assertThat(result).isEqualTo(stop);
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();

        when(stopRepo.getStopById(id)).thenReturn(null);

        assertThatThrownBy(() -> stopService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    void findByRouteIdAndTransportType_shouldReturnListsOfStops() {
        int number = 5;
        Transport type = Transport.BUS;

        Stop s1 = Stop.builder().name("Stop 1").build();
        Stop s2 = Stop.builder().name("Stop 2").build();

        Route route = Route.builder()
                .number(number)
                .transport(type)
                .stops(List.of(s1, s2))
                .build();

        when(routeService.findByNumber(number, type)).thenReturn(List.of(route));

        List<List<Stop>> result = stopService.findByRouteIdAndTransportType(number, type);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).containsExactly(s1, s2);
        verify(routeService).findByNumber(number, type);
    }

    @Test
    void findByRouteIdAndTransportType_shouldReturnMultipleStopLists_whenMultipleRoutesExist() {
        int number = 5;
        Transport type = Transport.BUS;

        Stop s1 = Stop.builder().name("Остановка А").build();
        Stop s2 = Stop.builder().name("Остановка Б").build();
        Stop s3 = Stop.builder().name("Остановка В").build();

        Route routeDirect = Route.builder()
                .number(number).transport(type)
                .stops(List.of(s1, s2)).build();

        Route routeBack = Route.builder()
                .number(number).transport(type)
                .stops(List.of(s2, s3)).build();

        when(routeService.findByNumber(number, type)).thenReturn(List.of(routeDirect, routeBack));

        List<List<Stop>> result = stopService.findByRouteIdAndTransportType(number, type);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly(s1, s2);
        assertThat(result.get(1)).containsExactly(s2, s3);

        verify(routeService).findByNumber(number, type);
    }

    @Test
    void findByRouteIdAndTransportType_shouldReturnEmptyList_whenNoRoutesFound() {
        when(routeService.findByNumber(999, Transport.BUS)).thenReturn(Collections.emptyList());

        List<List<Stop>> result = stopService.findByRouteIdAndTransportType(999, Transport.BUS);

        assertThat(result).isEmpty();
    }
}