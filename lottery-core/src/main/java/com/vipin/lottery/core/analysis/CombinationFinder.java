package com.vipin.lottery.core.analysis;

import java.util.*;

final class CombinationFinder {
    public Map<String, Object> find(int maximum, int sum, Integer deviation) {
        if (maximum < 5
                || maximum > 75
                || sum < 15
                || sum > 5 * maximum - 10
                || deviation != null && (deviation < 0 || deviation > 40))
            throw new IllegalArgumentException(
                    "Choose maximum 5–75, a feasible sum, and deviation 0–40");
        List<List<Integer>> rows = new ArrayList<>();
        search(new ArrayList<>(), 1, maximum, sum, deviation, rows);
        boolean truncated = rows.size() > 500;
        return Map.of("rows", rows.subList(0, Math.min(500, rows.size())), "truncated", truncated);
    }

    private void search(
            List<Integer> chosen,
            int start,
            int maximum,
            int remaining,
            Integer deviation,
            List<List<Integer>> result) {
        if (result.size() > 500) return;
        int left = 5 - chosen.size();
        if (left == 0) {
            if (remaining == 0) {
                double mean = chosen.stream().mapToInt(Integer::intValue).sum() / 5.0;
                int sd =
                        (int)
                                Math.floor(
                                        Math.sqrt(
                                                chosen.stream()
                                                                .mapToDouble(
                                                                        n ->
                                                                                (n - mean)
                                                                                        * (n
                                                                                                - mean))
                                                                .sum()
                                                        / 5));
                if (deviation == null || deviation == sd) result.add(List.copyOf(chosen));
            }
            return;
        }
        if (start + left - 1 > maximum
                || remaining < left * start + left * (left - 1) / 2
                || remaining > left * maximum - left * (left - 1) / 2) return;
        for (int n = start; n <= maximum - left + 1; n++) {
            chosen.add(n);
            search(chosen, n + 1, maximum, remaining - n, deviation, result);
            chosen.remove(chosen.size() - 1);
            if (result.size() > 500) return;
        }
    }
}
