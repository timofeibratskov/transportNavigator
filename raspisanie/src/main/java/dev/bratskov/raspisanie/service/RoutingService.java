package dev.bratskov.raspisanie.service;

import dev.bratskov.raspisanie.dto.RoutingRequestDto;
import dev.bratskov.raspisanie.dto.RoutingResponseDto;
import dev.bratskov.raspisanie.dto.PathDto;
import dev.bratskov.raspisanie.dto.SegmentDto;
import dev.bratskov.raspisanie.service.raptor.Raptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class RoutingService {

    private final Raptor raptor;

    public RoutingResponseDto findPath(RoutingRequestDto query) {
        var list = raptor.plan(query.originStopId(),
                query.targetStopId(),
                query.time(),
                query.day()
        );
        return buildResponse(list);
    }

    public List<RoutingResponseDto> findAllPaths(RoutingRequestDto query) {
        List<List<PathDto>> paths = raptor.planAllPaths(
                query.originStopId(),
                query.targetStopId(),
                query.time(),
                query.day()
        );

        return paths.stream()
                .sorted(Comparator.comparing(path -> {
                    LocalTime start = path.getFirst().time();
                    LocalTime end = path.getLast().time();
                    return calculateDuration(start, end);
                }))
                .limit(5)
                .map(this::buildResponse)
                .toList();
    }

    private RoutingResponseDto buildResponse(List<PathDto> fullPath) {
        if (fullPath.isEmpty()) {
            throw new IllegalStateException("Путь не может быть пустым");
        }

        LocalTime startTime = fullPath.getFirst().time();
        LocalTime endTime = fullPath.getLast().time();

        Duration duration = calculateDuration(startTime, endTime);

        List<SegmentDto> segments = buildSegments(fullPath);

        return RoutingResponseDto.builder()
                .fullPath(fullPath)
                .segments(segments)
                .transfers(segments.size() - 1)
                .totalStops((int) fullPath.stream().map(PathDto::stop).distinct().count())
                .routeTime(formatDuration(duration))
                .build();
    }

    private List<SegmentDto> buildSegments(List<PathDto> fullPath) {
        List<SegmentDto> segments = new ArrayList<>();

        int segmentStart = 0;
        String currentDirection = fullPath.getFirst().directionName();
        int currentNumber = fullPath.getFirst().number();

        for (int i = 1; i < fullPath.size(); i++) {
            PathDto current = fullPath.get(i);

            if (!current.directionName().equals(currentDirection) ||
                    current.number() != (currentNumber)) {

                segments.add(createSegment(fullPath, segmentStart, i - 1));

                segmentStart = i;
                currentDirection = current.directionName();
                currentNumber = current.number();
            }
        }

        segments.add(createSegment(fullPath, segmentStart, fullPath.size() - 1));

        return segments;
    }

    private SegmentDto createSegment(List<PathDto> fullPath, int startIdx, int endIdx) {
        PathDto boarding = fullPath.get(startIdx);
        PathDto exit = fullPath.get(endIdx);
        int stopsCount = endIdx - startIdx + 1;

        return SegmentDto.builder()
                .boardingStop(boarding.stop())
                .boardingTime(boarding.time())
                .exitStop(exit.stop())
                .exitTime(exit.time())
                .transport(boarding.transport())
                .routeNumber(boarding.number())
                .direction(boarding.directionName())
                .stopsCount(stopsCount)
                .build();
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", hours, minutes);
    }

    private Duration calculateDuration(LocalTime start, LocalTime end) {
        if (end.isBefore(start)) {
            Duration tillMidnight = Duration.between(start, LocalTime.MAX);
            Duration afterMidnight = Duration.between(LocalTime.MIDNIGHT, end);
            return tillMidnight.plus(afterMidnight).plusSeconds(1); // +1 секунда т.к. MAX это 23:59:59
        }
        return Duration.between(start, end);
    }
}
