package com.vipin.lottery.core.analysis;

import com.vipin.lottery.core.model.DrawRecord;
import java.util.*;
import java.util.stream.Collectors;

final class RangePatternAnalyzer {
    public List<Map<String, Object>> compare(List<DrawRecord> draws, String game) {
        int max = DrawRecord.maximum(game);
        int[] capacity = new int[max / 10 + 1];
        for (int n = 1; n <= max; n++) capacity[n / 10]++;
        Map<String, Integer> observed = new HashMap<>();
        for (DrawRecord d : draws) {
            int[] p = new int[capacity.length];
            d.whites().forEach(n -> p[n / 10]++);
            observed.merge(key(p), 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        patterns(capacity, new int[capacity.length], 0, 5, observed, result);
        return result;
    }

    private void patterns(
            int[] capacity,
            int[] p,
            int at,
            int left,
            Map<String, Integer> observed,
            List<Map<String, Object>> result) {
        if (at == p.length) {
            if (left == 0) {
                long ways = 1;
                int square = 0;
                for (int i = 0; i < p.length; i++) {
                    ways *= choose(capacity[i], p[i]);
                    square += p[i] * p[i];
                }
                result.add(
                        Map.of(
                                "pattern",
                                key(p),
                                "observed",
                                observed.getOrDefault(key(p), 0),
                                "combinations",
                                ways,
                                "square",
                                square));
            }
            return;
        }
        for (int n = 0; n <= Math.min(capacity[at], left); n++) {
            p[at] = n;
            patterns(capacity, p, at + 1, left - n, observed, result);
        }
    }

    private String key(int[] p) {
        return Arrays.stream(p).mapToObj(Integer::toString).collect(Collectors.joining("-"));
    }

    private long choose(int n, int k) {
        long r = 1;
        for (int i = 1; i <= k; i++) r = r * (n - i + 1) / i;
        return r;
    }
}
