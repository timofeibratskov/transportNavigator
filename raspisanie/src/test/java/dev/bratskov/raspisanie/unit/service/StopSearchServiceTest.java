package dev.bratskov.raspisanie.unit.service;

import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.repo.StopRepo;
import dev.bratskov.raspisanie.service.StopSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StopSearchServiceTest {

    @Mock
    private StopRepo stopRepo;

    @InjectMocks
    private StopSearchService stopSearchService;

    @BeforeEach
    void setUp() {
        Set<Stop> testStops = Set.of(
                Stop.builder()
                        .id(UUID.randomUUID())
                        .name("ТОРГОВЫЙ ЦЕНТР \"OLDCITY\"")
                        .build(),
                Stop.builder()
                        .id(UUID.randomUUID())
                        .name("УЛИЦА БОЛДИНА")
                        .build(),
                Stop.builder()
                        .id(UUID.randomUUID())
                        .name("ГОСАВТОИНСПЕКЦИЯ")
                        .build(),
                Stop.builder()
                        .id(UUID.randomUUID())
                        .name("СРЕДНЯЯ ШКОЛА №3")
                        .build()
        );

        when(stopRepo.getStops()).thenReturn(testStops);
    }

    @Test
    void searchByName_shouldFindExactMatchWithAlias() {
        List<Stop> result = stopSearchService.searchByName("ОЛД СИТИ");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().name()).isEqualTo("ТОРГОВЫЙ ЦЕНТР \"OLDCITY\"");
    }

    @Test
    void searchByName_shouldHandleFuzzyAliasMatch() {
        List<Stop> result = stopSearchService.searchByName("ОЛДСЫТИ");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().name()).isEqualTo("ТОРГОВЫЙ ЦЕНТР \"OLDCITY\"");
    }

    @Test
    void searchByName_shouldExpandAbbreviations() {
        List<Stop> result = stopSearchService.searchByName("ГАИ");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().name()).isEqualTo("ГОСАВТОИНСПЕКЦИЯ");
    }

    @Test
    void searchByName_shouldHandleFuzzyTypoInNormalName() {
        List<Stop> result = stopSearchService.searchByName("УЛИЦА БАЛДИНА");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().name()).isEqualTo("УЛИЦА БОЛДИНА");
    }

    @Test
    void searchByName_shouldReturnEmptyList_whenQueryIsTooShort() {
        List<Stop> result = stopSearchService.searchByName("");

        assertThat(result).isEmpty();
    }

    @Test
    void searchByName_shouldNormalizeInputAndDatabaseNames() {
        List<Stop> result = stopSearchService.searchByName("средняя школа");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().name()).contains("ШКОЛА");
    }

    @Test
    void searchByName_shouldPrioritizeExactMatches() {
        List<Stop> result = stopSearchService.searchByName("ГАИ");

        assertThat(result.getFirst().name()).isEqualTo("ГОСАВТОИНСПЕКЦИЯ");
    }

    @Test
    void searchByName_shouldLimitResults() {
        List<Stop> result = stopSearchService.searchByName("А");

        assertThat(result.size()).isLessThanOrEqualTo(10);
    }
}