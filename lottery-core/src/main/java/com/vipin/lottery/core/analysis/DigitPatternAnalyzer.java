package com.vipin.lottery.core.analysis;

import com.vipin.lottery.core.model.DrawRecord;
import java.util.*;
import java.util.stream.Collectors;

final class DigitPatternAnalyzer {
    public Map<String, Object> analyze(List<DrawRecord> draws, int window) {
        if (window < 1 || window > 100) throw new IllegalArgumentException("Window must be 1–100");
        if (draws.stream().anyMatch(d -> d.special() == null))
            throw new IllegalArgumentException(
                    "Digit analysis requires special balls for every selected draw");
        String digits =
                draws.stream().map(d -> d.values().replace(" ", "")).collect(Collectors.joining());
        if (window > digits.length())
            throw new IllegalArgumentException("Window exceeds selected digit-stream length");
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i <= digits.length() - window; i++)
            counts.merge(digits.substring(i, i + window), 1, Integer::sum);
        var sorted =
                counts.entrySet().stream()
                        .sorted(
                                Map.Entry.<String, Integer>comparingByValue()
                                        .reversed()
                                        .thenComparing(Map.Entry.comparingByKey()))
                        .toList();
        return Map.of(
                "digits",
                digits.length(),
                "windows",
                digits.length() - window + 1,
                "unique",
                counts.size(),
                "rows",
                sorted.stream()
                        .limit(1000)
                        .map(e -> Map.of("pattern", e.getKey(), "count", e.getValue()))
                        .toList(),
                "truncated",
                sorted.size() > 1000);
    }
}
