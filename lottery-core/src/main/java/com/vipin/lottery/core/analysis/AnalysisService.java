package com.vipin.lottery.core.analysis;

import com.vipin.lottery.core.model.DrawRecord;
import java.util.*;

public class AnalysisService {
    public Map<String, Object> analyze(List<DrawRecord> draws, String game) {
        Statistics stats = new Statistics(DrawRecord.maximum(game));
        List<Map<String, Object>> sums = new ArrayList<>();
        Map<Integer, Integer> sumCounts = new TreeMap<>(),
                deviationCounts = new TreeMap<>(),
                first = new TreeMap<>(),
                last = new TreeMap<>(),
                ends = new TreeMap<>();
        Map<String, List<String>> combinations = new LinkedHashMap<>();
        long total = 0;
        for (DrawRecord d : draws) {
            stats.accept(d);
            total += d.sum();
            sums.add(
                    Map.of(
                            "date",
                            d.date(),
                            "whites",
                            d.whites(),
                            "sum",
                            d.sum(),
                            "mean",
                            d.sum() / 5.0,
                            "deviation",
                            d.deviation(),
                            "runningTotal",
                            total,
                            "runningAverage",
                            (double) total / (sums.size() + 1)));
            sumCounts.merge(d.sum(), 1, Integer::sum);
            deviationCounts.merge((int) Math.floor(d.deviation()), 1, Integer::sum);
            first.merge(d.whites().get(0), 1, Integer::sum);
            last.merge(d.whites().get(4), 1, Integer::sum);
            ends.merge(d.whites().get(0) + d.whites().get(4), 1, Integer::sum);
            combinations
                    .computeIfAbsent(d.values(), k -> new ArrayList<>())
                    .add(d.date().toString());
        }
        List<Map<String, Object>> ranges = new ArrayList<>();
        var numbers = stats.numbers();
        for (int i = 0; i <= DrawRecord.maximum(game) / 10; i++) {
            final int bucket = i;
            ranges.add(
                    Map.of(
                            "range",
                            stats.rangeLabel(i),
                            "total",
                            numbers.stream()
                                    .filter(n -> n.getNumber() / 10 == bucket)
                                    .mapToLong(NumberStats::getTotal)
                                    .sum(),
                            "since",
                            numbers.stream()
                                    .filter(n -> n.getNumber() / 10 == bucket)
                                    .mapToLong(NumberStats::getSince)
                                    .sum()));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", draws.size());
        result.put("latest", draws.isEmpty() ? null : draws.get(draws.size() - 1));
        result.put("totalSum", total);
        result.put("averageSum", draws.isEmpty() ? 0 : (double) total / draws.size());
        result.put("numbers", numbers);
        result.put("sums", sums);
        result.put("sumCounts", sumCounts);
        result.put("deviations", deviationCounts);
        result.put("first", first);
        result.put("last", last);
        result.put("endSums", ends);
        result.put("ranges", ranges);
        result.put("patterns", stats.patterns());
        result.put("shapes", stats.shapes());
        result.put("occupancies", stats.occupancies());
        result.put(
                "repeated",
                combinations.entrySet().stream()
                        .filter(e -> e.getValue().size() > 1)
                        .map(e -> Map.of("balls", e.getKey(), "dates", e.getValue()))
                        .toList());
        return result;
    }

    public Map<String, Object> digits(List<DrawRecord> draws, int window) {
        return new DigitPatternAnalyzer().analyze(draws, window);
    }

    public List<Map<String, Object>> timeline(List<DrawRecord> draws, int number, String game) {
        if (number < 1 || number > DrawRecord.maximum(game))
            throw new IllegalArgumentException("Number out of range");
        List<Map<String, Object>> result = new ArrayList<>();
        int previous = 0, total = 0;
        for (int i = 0; i < draws.size(); i++) {
            DrawRecord d = draws.get(i);
            boolean hit = d.whites().contains(number);
            if (hit) {
                total++;
                previous = i + 1;
            }
            result.add(
                    Map.of(
                            "date",
                            d.date(),
                            "draw",
                            i + 1,
                            "appeared",
                            hit,
                            "total",
                            total,
                            "since",
                            i + 1 - previous));
        }
        return result;
    }

    public Map<String, Object> combinations(int maximum, int sum, Integer deviation) {
        return new CombinationFinder().find(maximum, sum, deviation);
    }

    public List<Map<String, Object>> rangeUniverse(List<DrawRecord> draws, String game) {
        return new RangePatternAnalyzer().compare(draws, game);
    }
}
