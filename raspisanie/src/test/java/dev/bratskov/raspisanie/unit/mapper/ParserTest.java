package dev.bratskov.raspisanie.unit.mapper;

import dev.bratskov.raspisanie.exception.DataInitializationException;
import dev.bratskov.raspisanie.mapper.Parser;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.Trip;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.reader.TextFileReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParserTest {

    @Mock
    private TextFileReader reader;

    private Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Parser(reader);
        ReflectionTestUtils.setField(parser, "filePath", "test-data.txt");
    }

    @Test
    void init_shouldParseRealFormatData_correctly() throws IOException {
        String testData = """
                ТРАНСПОРТ: АВТОБУС --
                НОМЕР: 1 -- НАПРАВЛЕНИЕ: ВИШНЕВЕЦ - ЗАВОД -- ДЕНЬ: Будний -- ОСТАНОВКИ: ВИШНЕВЕЦ (КОНЕЧНАЯ)_КОНЕЧНАЯ -> ПОЛИКЛИНИКА № 7 _УЛИЦА КАБЯКА -> ПОЧТА _ВИШНЕВЕЦ-1 --
                06:10-06:12-06:14
                06:48-06:50-06:52
                """;

        when(reader.read("test-data.txt")).thenReturn(testData);

        parser.init();

        Map<String, Stop> stops = parser.getStopMap();
        Map<String, Route> routes = parser.getRouteMap();
        List<Trip> trips = parser.getTrips();

        assertThat(stops).hasSize(3);
        assertThat(stops).containsKey("ВИШНЕВЕЦ _КОНЕЧНАЯ");

        assertThat(routes).hasSize(1);
        Route route = routes.values().iterator().next();
        assertThat(route.number()).isEqualTo(1);
        assertThat(route.transport()).isEqualTo(Transport.BUS);
        assertThat(route.direction()).isEqualTo("ВИШНЕВЕЦ - ЗАВОД");

        assertThat(trips).hasSize(2);
        assertThat(trips.getFirst().day()).isEqualTo(Day.WEEKDAY);
    }

    @Test
    void init_shouldParseTrolleybusAndWeekend() throws IOException {
        String testData = """
                ТРАНСПОРТ: ТРОЛЛЕЙБУС -- НОМЕР: 2 -- НАПРАВЛЕНИЕ: КСМ - ГРОДНО АЗОТ -- ДЕНЬ: Выходной -- ОСТАНОВКИ: КСМ -> ГРОНИТЕКС -> АВТОВОКЗАЛ -- 10:00-10:05-10:10
                """;

        when(reader.read("test-data.txt")).thenReturn(testData);

        parser.init();

        Route route = parser.getRouteMap().values().iterator().next();
        List<Trip> trips = parser.getTrips();

        assertThat(route.transport()).isEqualTo(Transport.TROLLEYBUS);
        assertThat(route.number()).isEqualTo(2);

        assertThat(trips).hasSize(1);
        assertThat(trips.getFirst().day()).isEqualTo(Day.WEEKEND);
    }

    @Test
    void init_shouldCleanStopNames_removeUnderscoreSuffix() throws IOException {
        String testData = """
                ТРАНСПОРТ: АВТОБУС -- НОМЕР: 1 -- НАПРАВЛЕНИЕ: A - B -- ДЕНЬ: Будний -- ОСТАНОВКИ: ПОЧТА _ВИШНЕВЕЦ-1 -> МАГАЗИН "КВАСОВСКИЙ" _ПРОСПЕКТ КЛЕЦКОВА -- 08:00-08:05
                """;

        when(reader.read("test-data.txt")).thenReturn(testData);

        parser.init();

        Map<String, Stop> stops = parser.getStopMap();

        Stop stop1 = stops.get("ПОЧТА _ВИШНЕВЕЦ-1");
        Stop stop2 = stops.get("МАГАЗИН \"КВАСОВСКИЙ\" _ПРОСПЕКТ КЛЕЦКОВА");

        assertThat(stop1.name()).isEqualTo("ПОЧТА");
        assertThat(stop2.name()).isEqualTo("МАГАЗИН \"КВАСОВСКИЙ\"");
    }

    @Test
    void init_shouldHandleComplexTimes_withNulls() throws IOException {
        String testData = """
                ТРАНСПОРТ: АВТОБУС -- НОМЕР: 5 -- НАПРАВЛЕНИЕ: A - B -- ДЕНЬ: Будний -- ОСТАНОВКИ: ОСТ1 -> ОСТ2 -> ОСТ3 -- 08:00-(XXX)-08:10
                """;

        when(reader.read("test-data.txt")).thenReturn(testData);

        parser.init();

        Trip trip = parser.getTrips().getFirst();

        assertThat(trip.stops()).hasSize(3);
        assertThat(trip.stops().get(0).time()).isEqualTo(LocalTime.of(8, 0));
        assertThat(trip.stops().get(1).time()).isNull();
        assertThat(trip.stops().get(2).time()).isEqualTo(LocalTime.of(8, 10));
    }

    @Test
    void init_shouldCreateUniqueRoutes_forDifferentDirections() throws IOException {
        String testData = """
                ТРАНСПОРТ: АВТОБУС -- НОМЕР: 1 -- НАПРАВЛЕНИЕ: A - B -- ДЕНЬ: Будний -- ОСТАНОВКИ: A -> B -- 08:00-08:10
                ТРАНСПОРТ: АВТОБУС -- НОМЕР: 1 -- НАПРАВЛЕНИЕ: B - A -- ДЕНЬ: Будний -- ОСТАНОВКИ: B -> A -- 09:00-09:10
                """;

        when(reader.read("test-data.txt")).thenReturn(testData);

        parser.init();

        Map<String, Route> routes = parser.getRouteMap();

        assertThat(routes).hasSize(2);
        assertThat(routes.keySet()).anyMatch(k -> k.contains("A - B"));
        assertThat(routes.keySet()).anyMatch(k -> k.contains("B - A"));
    }

    @Test
    void init_shouldThrowException_whenFileIsEmptyOrNull() {
        ReflectionTestUtils.setField(parser, "filePath", null);
        assertThatThrownBy(() -> parser.init())
                .isInstanceOf(DataInitializationException.class);
    }

    @Test
    void init_shouldThrowException_whenFormatIsInvalid() throws IOException {
        String invalidData = "ПРОСТО КАКОЙ-ТО ТЕКСТ БЕЗ ФОРМАТА";
        when(reader.read("test-data.txt")).thenReturn(invalidData);

        assertThatThrownBy(() -> parser.init())
                .isInstanceOf(DataInitializationException.class)
                .hasMessageContaining("Не найдено");
    }
}