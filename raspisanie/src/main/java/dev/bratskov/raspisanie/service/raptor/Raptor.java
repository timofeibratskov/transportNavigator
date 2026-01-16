package dev.bratskov.raspisanie.service.raptor;

import dev.bratskov.raspisanie.dto.PathDto;
import dev.bratskov.raspisanie.model.Route;
import dev.bratskov.raspisanie.model.StopTime;
import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.model.Trip;
import dev.bratskov.raspisanie.model.enums.Day;
import dev.bratskov.raspisanie.repo.StopRepo;
import dev.bratskov.raspisanie.repo.TripRepo;
import dev.bratskov.raspisanie.service.StopService;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.UUID;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Collections;
import java.util.NoSuchElementException;

@Component
public class Raptor {

    private static final int MIN_TRANSFER_TIME = 4;
    private final Map<Stop, Set<Route>> routesAtStop;
    private final Map<Route, List<Trip>> tripsByRouteAtWeekend;
    private final Map<Route, List<Trip>> tripsByRouteAtWeekday;
    private final Map<Route, Map<Stop, Integer>> routeStopIndex;
    private final Map<Integer, Map<Stop, LocalTime>> kBestTimes = new HashMap<>();
    private final Map<Integer, Map<Stop, Trip>> previousTrip = new HashMap<>();
    private final Map<Integer, Map<Stop, Stop>> boardingStop = new HashMap<>();
    private final StopService stopService;

    public Raptor(StopRepo stopRepo,
                  StopService stopService,
                  TripRepo tripRepo) {
        this.stopService = stopService;
        Map<String, Stop> stopMap = stopRepo.getStopsMap();
        Map<Stop, Set<Route>> routesAtStopTmp = new HashMap<>();
        Map<Route, List<Trip>> tripsByRouteTmp = new HashMap<>();
        Map<Route, Map<Stop, Integer>> routeStopIndexTmp = new HashMap<>();
        Map<Route, List<Trip>> tripsByRouteWeekendTmp = new HashMap<>();
        Map<Route, List<Trip>> tripsByRouteWeekdayTmp = new HashMap<>();

        for (Trip original : tripRepo.getTrips()) {

            Route route = original.route();
            List<StopTime> unifiedStops = new ArrayList<>();

            Map<Stop, Integer> indexMap =
                    routeStopIndexTmp.computeIfAbsent(route, k -> new HashMap<>());

            int index = 0;
            for (StopTime st : original.stops()) {

                Stop unified = stopMap.get(st.stop().description());
                unifiedStops.add(
                        StopTime.builder()
                                .stop(unified)
                                .time(st.time())
                                .build()
                );

                routesAtStopTmp
                        .computeIfAbsent(unified, k -> new HashSet<>())
                        .add(route);

                indexMap.putIfAbsent(unified, index++);
            }

            Trip trip = Trip.builder()
                    .id(original.id())
                    .day(original.day())
                    .route(route)
                    .stops(unifiedStops)
                    .build();

            tripsByRouteTmp
                    .computeIfAbsent(route, k -> new ArrayList<>())
                    .add(trip);

            if (trip.day() == Day.WEEKDAY) {
                tripsByRouteWeekdayTmp.computeIfAbsent(route, k -> new ArrayList<>()).add(trip);
            } else {
                tripsByRouteWeekendTmp.computeIfAbsent(route, k -> new ArrayList<>()).add(trip);
            }
        }

        this.routesAtStop = Collections.unmodifiableMap(routesAtStopTmp);
        this.tripsByRouteAtWeekend = Collections.unmodifiableMap(tripsByRouteWeekendTmp);
        this.tripsByRouteAtWeekday = Collections.unmodifiableMap(tripsByRouteWeekdayTmp);

        Map<Route, Map<Stop, Integer>> frozen = new HashMap<>();
        for (var e : routeStopIndexTmp.entrySet()) {
            frozen.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
        }
        this.routeStopIndex = Collections.unmodifiableMap(frozen);
    }

    public List<List<PathDto>> planAllPaths(UUID originId,
                                            UUID targetId,
                                            LocalTime startTime,
                                            Day day) {
        if (originId.equals(targetId)) {
            throw new IllegalArgumentException(
                    "Начальная и конечная остановки совпадают!"
            );
        }

        Stop origin = stopService.findById(originId);
        Stop target = stopService.findById(targetId);

        runRaptorAlgorithm(origin, target, startTime, day, false);
        return buildAllPaths(origin, target);
    }

    public List<PathDto> plan(UUID originId, UUID targetId, LocalTime startTime, Day day) {
        if (originId.equals(targetId)) {
            throw new IllegalArgumentException(
                    "Начальная и конечная остановки совпадают!"
            );
        }

        Stop origin = stopService.findById(originId);
        Stop target = stopService.findById(targetId);

        runRaptorAlgorithm(origin, target, startTime, day, true);

        int k = findFirstRoundWithTarget(target);
        return buildPath(origin, target, k);
    }

    private void runRaptorAlgorithm(Stop origin,
                                    Stop target,
                                    LocalTime startTime,
                                    Day day,
                                    boolean stopOnFirstFound) {
        Map<Stop, LocalTime> bestTimes = new HashMap<>();
        Set<Stop> markedStops = new HashSet<>();

        bestTimes.put(origin, startTime);
        markedStops.add(origin);

        kBestTimes.clear();
        previousTrip.clear();
        boardingStop.clear();

        kBestTimes.put(0, Map.of(origin, startTime));
        previousTrip.put(0, new HashMap<>());
        boardingStop.put(0, new HashMap<>());

        for (int k = 1; !markedStops.isEmpty(); k++) {
            Map<Stop, LocalTime> roundTimes = new HashMap<>();
            Map<Stop, Trip> prevTripThisRound = new HashMap<>();
            Map<Stop, Stop> boardingStopThisRound = new HashMap<>();
            Set<Route> scannedRoutes = new HashSet<>();

            Queue<Stop> queue = new ArrayDeque<>(markedStops);
            markedStops.clear();

            while (!queue.isEmpty()) {

                Stop stop = queue.poll();
                LocalTime bestTimeAtStop = bestTimes.get(stop);

                for (Route route : routesAtStop.getOrDefault(stop, Set.of())) {

                    if (!scannedRoutes.add(route)) {  // пропускаем дубликаты
                        continue;
                    }

                    Trip previousTripAtStop = null;
                    if (k > 1 && previousTrip.containsKey(k - 1)) {
                        previousTripAtStop = previousTrip.get(k - 1).get(stop);
                    }

                    scanRoute(
                            route,
                            stop,
                            bestTimeAtStop,
                            bestTimes,
                            roundTimes,
                            prevTripThisRound,
                            boardingStopThisRound,
                            day,
                            previousTripAtStop,
                            target
                    );
                }
            }

            for (var e : roundTimes.entrySet()) {
                Stop stop = e.getKey();
                LocalTime time = e.getValue();

                if (!bestTimes.containsKey(stop) || time.isBefore(bestTimes.get(stop))) {
                    bestTimes.put(stop, time);
                    markedStops.add(stop);
                }
            }

            kBestTimes.put(k, roundTimes);
            previousTrip.put(k, prevTripThisRound);
            boardingStop.put(k, boardingStopThisRound);

            if (stopOnFirstFound && roundTimes.containsKey(target)) {
                break;
            }
        }
    }

    private void scanRoute(
            Route route,
            Stop boardingCandidateStop,
            LocalTime arrivalAtStop,
            Map<Stop, LocalTime> bestTimes,
            Map<Stop, LocalTime> roundTimes,
            Map<Stop, Trip> prevTrip,
            Map<Stop, Stop> boardingStop,
            Day day,
            Trip previousTripAtThisStop,
            Stop target
    ) {
        Trip trip = day.equals(Day.WEEKDAY)
                ? getEarliestTrip(route, boardingCandidateStop, arrivalAtStop,
                tripsByRouteAtWeekday, previousTripAtThisStop, boardingCandidateStop)
                : getEarliestTrip(route, boardingCandidateStop, arrivalAtStop,
                tripsByRouteAtWeekend, previousTripAtThisStop, boardingCandidateStop);

        if (trip == null) return;

        int startIndex = routeStopIndex.get(route).get(boardingCandidateStop);
        LocalTime targetBestTime = bestTimes.get(target);

        for (int i = startIndex; i < trip.stops().size(); i++) {
            StopTime st = trip.stops().get(i);
            Stop stop = st.stop();
            LocalTime time = st.time();
            if (time == null) continue;

            if (targetBestTime != null && !time.isBefore(targetBestTime)) {
                break;
            }

            boolean improvesCurrentStop = bestTimes.get(stop) == null || time.isBefore(bestTimes.get(stop));
            boolean improvesRoundStop = roundTimes.get(stop) == null || time.isBefore(roundTimes.get(stop));

            if (improvesCurrentStop && improvesRoundStop) {
                roundTimes.put(stop, time);
                prevTrip.put(stop, trip);
                boardingStop.put(stop, boardingCandidateStop);
            }
        }
    }

    private Trip getEarliestTrip(
            Route route,
            Stop stop,
            LocalTime arrivalTime,
            Map<Route, List<Trip>> tripsByRoute,
            Trip previousTrip,
            Stop previousStop
    ) {
        int index = routeStopIndex.get(route).get(stop);
        List<Trip> trips = tripsByRoute.get(route);
        if (trips == null) return null;

        int transferTime = calculateTransferTime(route, previousTrip, previousStop);
        LocalTime earliestDeparture = arrivalTime.plusMinutes(transferTime);

        for (Trip trip : trips) {
            LocalTime time = trip.stops().get(index).time();
            if (time == null) continue;

            if (!time.isBefore(earliestDeparture)) {
                return trip;
            }
        }
        return null;
    }

    private int calculateTransferTime(Route route, Trip previousTrip, Stop previousStop) {
        if (previousTrip == null) {
            return MIN_TRANSFER_TIME;
        }

        boolean sameTransport = previousTrip.route().transport() == route.transport();
        boolean sameNumber = previousTrip.route().number().equals(route.number());

        if (sameTransport && sameNumber) {
            List<StopTime> prevStops = previousTrip.stops();
            StopTime lastStopTime = prevStops.getLast();

            //для конечных остановок, где транспорт меняет направление и сразу начинает новый маршрут
            if (lastStopTime.stop().equals(previousStop)) {
                return 0;
            }
        }

        return MIN_TRANSFER_TIME;
    }

    private List<List<PathDto>> buildAllPaths(Stop origin, Stop target) {
        List<List<PathDto>> allPaths = new ArrayList<>();

        for (var entry : kBestTimes.entrySet()) {
            int k = entry.getKey();
            if (!entry.getValue().containsKey(target)) {
                continue;
            }

            try {
                List<PathDto> path = buildPath(origin, target, k);


                boolean isUnique = true;
                for (List<PathDto> existing : allPaths) {
                    if (pathsAreEqual(existing, path)) {
                        isUnique = false;
                        break;
                    }
                }

                if (isUnique) {
                    allPaths.add(path);
                }
            } catch (Exception e) {
                // Путь не построен
            }
        }

        if (allPaths.isEmpty()) {
            throw new NoSuchElementException(
                    "Маршрут между остановками не найден. Возможно, они не связаны транспортом или нет рейсов в указанное время"
            );
        }

        return allPaths;
    }

    private boolean pathsAreEqual(List<PathDto> path1, List<PathDto> path2) {
        if (path1.size() != path2.size()) {
            return false;
        }

        for (int i = 0; i < path1.size(); i++) {
            PathDto p1 = path1.get(i);
            PathDto p2 = path2.get(i);

            if (!p1.stop().equals(p2.stop()) ||
                    !p1.time().equals(p2.time()) ||
                    !p1.directionName().equals(p2.directionName()) ||
                    p1.number() != p2.number()) {
                return false;
            }
        }

        return true;
    }

    private List<PathDto> buildPath(Stop origin, Stop target, int k) {
        LinkedList<PathDto> path = new LinkedList<>();

        Stop current = target;

        while (k > 0 && !current.equals(origin)) {

            Trip trip = previousTrip.get(k).get(current);
            Stop boarding = boardingStop.get(k).get(current);

            int exitIndex = routeStopIndex.get(trip.route()).get(current);
            int entryIndex = routeStopIndex.get(trip.route()).get(boarding);

            for (int i = exitIndex; i >= entryIndex; i--) {
                StopTime st = trip.stops().get(i);
                path.addFirst(
                        PathDto.builder()
                                .stop(st.stop())
                                .time(st.time())
                                .transport(trip.route().transport())
                                .number(trip.route().number())
                                .directionName(trip.route().direction())
                                .build()
                );
            }
            current = boarding;
            k--;
        }
        return path;
    }

    private int findFirstRoundWithTarget(Stop target) {
        return kBestTimes.entrySet().stream()
                .filter(e -> e.getValue().containsKey(target))
                .mapToInt(Map.Entry::getKey)
                .min()
                .orElseThrow(() -> new NoSuchElementException("Маршрут не найден"));
    }
}