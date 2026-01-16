package dev.bratskov.raspisanie.service;

import dev.bratskov.raspisanie.model.Stop;
import dev.bratskov.raspisanie.repo.StopRepo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StopSearchService {

    private final StopRepo stopRepo;

    private static final int MIN_QUERY_LENGTH = 1;
    private static final int MAX_RESULTS = 10;

    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();

    private static final Map<String, String> aliasMap = Map.ofEntries(
            Map.entry("СШ", "СРЕДНЯЯ ШКОЛА"),
            Map.entry("ГАИ", "ГОСАВТОИНСПЕКЦИЯ"),
            Map.entry("ШК", "ШКОЛА"),
            Map.entry("ТРК", "ТОРГОВО РАЗВЛЕКАТЕЛЬНЫЙ КОМПЛЕКС"),
            Map.entry("ТЦ", "ТОРГОВЫЙ ЦЕНТР"),
            Map.entry("ОЛДСИТИ", "ТОРГОВЫЙ ЦЕНТР OLDCITY"),
            Map.entry("ОЛД СИТИ", "ТОРГОВЫЙ ЦЕНТР OLDCITY")
    );

    public List<Stop> searchByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }

        String query = normalize(name);

        if (query.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        // Расширяем запрос (СШ -> СРЕДНЯЯ ШКОЛА)
        query = expandQuery(query);

        String finalQuery = query;

        return stopRepo.getStops().stream()
                .map(stop -> new StopScore(stop, calculateScore(stop, finalQuery)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparing(StopScore::score).reversed())
                .limit(MAX_RESULTS)
                .map(StopScore::stop)
                .toList();
    }

    private String expandQuery(String query) {
        for (var entry : aliasMap.entrySet()) {
            if (query.equals(entry.getKey())) {
                return normalize(entry.getValue());
            }
        }

        String[] words = query.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            String processed = word;
            for (var entry : aliasMap.entrySet()) {

                int dist = LEVENSHTEIN.apply(word, entry.getKey());
                if (dist <= Math.max(1, entry.getKey().length() / 3)) {
                    processed = entry.getValue();
                    break;
                }
            }
            result.append(processed).append(" ");
        }
        return normalize(result.toString());
    }

    private int calculateScore(Stop stop, String query) {
        String stopName = normalize(stop.name());

        if (stopName.equals(query)) return 100000;
        if (stopName.startsWith(query)) return 50000 + (query.length() * 100);
        if (stopName.contains(query)) return 30000 + (query.length() * 100);

        int wordScore = checkWordMatches(stopName, query);
        if (wordScore > 0) return wordScore;

        return checkFuzzyMatch(stopName, query);
    }

    private int checkWordMatches(String stopName, String query) {
        String[] stopWords = stopName.split("\\s+");
        String[] queryWords = query.split("\\s+");
        int score = 0;
        int matches = 0;

        for (String qW : queryWords) {
            if (qW.length() < 2) continue;
            for (String sW : stopWords) {
                if (sW.equals(qW)) {
                    score += 5000;
                    matches++;
                    break;
                }
                if (sW.startsWith(qW)) {
                    score += 2000;
                    matches++;
                    break;
                }
            }
        }
        return (matches == queryWords.length) ? score + 10000 : score;
    }

    private int checkFuzzyMatch(String stopName, String query) {
        int dist = LEVENSHTEIN.apply(query, stopName);
        if (dist <= Math.max(1, query.length() / 3)) {
            return 2000 - (dist * 100);
        }
        return 0;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toUpperCase()
                .replace("Ё", "Е")
                .replaceAll("[^А-ЯЁA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record StopScore(
            Stop stop,
            int score
    ) {
    }
}