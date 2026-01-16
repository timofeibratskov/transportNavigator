package dev.bratskov.raspisanie.mapper;

import dev.bratskov.raspisanie.exception.DataInitializationException;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.enums.Transport;
import dev.bratskov.raspisanie.model.Trip;
import dev.bratskov.raspisanie.model.StopTime;
import dev.bratskov.raspisanie.reader.TextFileReader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Arrays;

@Slf4j
@Component
public class Parser {
    private final Map<String, Stop> stopMap;
    private final Map<String, Route> routeMap;
    private final TextFileReader reader;
    private final List<Trip> trips;
    @Value("${data.trips-file}")
    private String filePath;

    @PostConstruct
    public void init() {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new DataInitializationException(
                    "Не указан путь к файлу данных. Проверьте параметр 'data.trips-file'"
            );
        }

        try {
            log.info("Загрузка данных из файла: {}", filePath);
            String text = reader.read(filePath);
            parse(text);

            if (stopMap.isEmpty()) {
                throw new DataInitializationException(
                        "Не найдено ни одной остановки в файле: " + filePath
                );
            }

            if (routeMap.isEmpty()) {
                throw new DataInitializationException(
                        "Не найдено ни одного маршрута в файле: " + filePath
                );
            }

            if (trips.isEmpty()) {
                throw new DataInitializationException(
                        "Не найдено ни одного рейса в файле: " + filePath
                );
            }

            log.info("Данные успешно загружены. Остановок: {}, Маршрутов: {}, Рейсов: {}",
                    stopMap.size(), routeMap.size(), trips.size());
        } catch (IOException e) {
            log.error("Ошибка при чтении файла данных: {}", filePath, e);
            throw new DataInitializationException(
                    "Не удалось загрузить данные из файла: " + filePath, e);
        } catch (DataInitializationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при парсинге данных из файла: {}", filePath, e);
            throw new DataInitializationException(
                    "Ошибка при обработке данных из файла: " + filePath, e);
        }
    }

    public Parser(TextFileReader reader) {
        this.routeMap = new HashMap<>();
        this.stopMap = new HashMap<>();
        this.trips = new ArrayList<>();
        this.reader = reader;
    }

    public Map<String, Stop> getStopMap() {
        return new HashMap<>(Map.copyOf(this.stopMap));
    }

    public Map<String, Route> getRouteMap() {
        return new HashMap<>(Map.copyOf(this.routeMap));
    }

    public List<Trip> getTrips() {
        return new ArrayList<>(List.copyOf(this.trips));
    }

    private void parse(String text) {
        String[] splitByTransport = text.split("ТРАНСПОРТ: ");
        for (String s : splitByTransport) {
            String[] block = s.split("--");
            if (block.length == 1)
                continue;

            Transport transportType = block[0].trim().length() == 7 ? Transport.BUS : Transport.TROLLEYBUS;

            int number = Integer.parseInt(block[1].split("НОМЕР:")[1].trim());

            String direction = block[2].split("НАПРАВЛЕНИЕ: ")[1].trim();

            Day day = block[3].split("ДЕНЬ: ")[1].trim().length() == 6 ? Day.WEEKDAY : Day.WEEKEND;

            List<String> stringStops = Arrays.stream(block[4].split("ОСТАНОВКИ: ")[1].split(" -> ")).toList();
            List<Stop> stops = generateStops(stringStops);

            List<String> stringTimes = Arrays.stream(block[5].split("\n")).toList();

            String uniqKey = number + "_" + direction;
            Route route = routeMap.computeIfAbsent(uniqKey, k -> Route.builder().id(UUID.randomUUID()).direction(direction).number(number).stops(stops).transport(transportType).build());

            generateStopTimes(stops, stringTimes, route, day);
        }
    }

    private void generateTrips(Route route, Day day, List<StopTime> stopTimes) {
        this.trips.add(
                Trip.builder()
                        .id(UUID.randomUUID())
                        .route(route)
                        .stops(stopTimes)
                        .day(day)
                        .build()
        );
    }

    private List<Stop> generateStops(List<String> stringStops) {
        List<Stop> stops = new ArrayList<>();
        stringStops
                .forEach(stringStop -> {
                            stringStop = stringStop.replace("(КОНЕЧНАЯ)", "").trim();
                            String uniqueStopDescription = stringStop;

                            stringStop = stringStop.split((" _"))[0].trim();

                            String finalStringStop = stringStop;

                            Stop stop = stopMap.computeIfAbsent(uniqueStopDescription, description -> Stop.builder()
                                    .id(UUID.randomUUID())
                                    .name(finalStringStop)
                                    .description(description)
                                    .build()
                            );
                            stops.add(stop);
                        }
                );
        return stops;
    }

    private void generateStopTimes(List<Stop> stops, List<String> stringTimes, Route route, Day day) {
        for (String stringTime : stringTimes) {

            List<String> parts = Arrays.stream(stringTime.split("-"))
                    .map(String::trim)
                    .toList();

            if (parts.size() != stops.size()) {
                continue;
            }

            List<StopTime> stopTimes = new ArrayList<>();

            for (int i = 0; i < parts.size(); i++) {
                LocalTime time = parseTimeOrNull(parts.get(i));

                stopTimes.add(
                        StopTime.builder()
                                .stop(stops.get(i))
                                .time(time)
                                .build()
                );
            }
            generateTrips(route, day, stopTimes);
        }
    }

    private LocalTime parseTimeOrNull(String value) {
        if (value == null) return null;

        value = value.trim();

        if (value.isEmpty()) return null;
        if (value.equals("(XXX)")) return null;

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
