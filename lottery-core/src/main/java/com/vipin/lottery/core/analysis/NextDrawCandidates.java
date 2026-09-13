package com.vipin.lottery.core.analysis;

import com.vipin.lottery.core.model.DrawRecord;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/** Descriptive constraint search, not an estimate of winning probabilities. */
public final class NextDrawCandidates {
    private static final int LIMIT = 200;

    public record Mode(int value, int count) {}

    public record Candidate(
            List<Integer> whites, int sum, double deviation, int deviationFloor, String pattern) {}

    public record Result(
            int trainingCount,
            List<Mode> sumModes,
            List<Mode> deviationModes,
            int unseenPatterns,
            List<Candidate> rows,
            boolean truncated,
            long totalCandidates,
            Backtest backtest) {}

    public record LaterDraw(int drawNumber, DrawRecord draw) {}

    public record Comparison(
            int drawNumber,
            LocalDate date,
            int drawsAfterCutoff,
            List<Integer> actual,
            List<Integer> candidate,
            List<Integer> shared,
            int matchedBalls) {}

    public record Backtest(
            int checkedDraws,
            int exactMatches,
            int closestMatchedBalls,
            List<Comparison> exact,
            List<Comparison> closest,
            List<Comparison> perDraw) {}

    private record Match(long ordinal, List<Integer> whites) {}

    /** Keep only subsets required by later draws, rather than materializing every candidate. */
    private static final class Collector {
        long total;
        final List<Candidate> preview = new ArrayList<>();
        final Map<Long, Match> subsets = new HashMap<>();
        final Consumer<Candidate> sink;

        Collector(List<LaterDraw> later, Consumer<Candidate> sink) {
            this.sink = sink;
            if (!later.isEmpty()) subsets.put(0L, null);
            for (LaterDraw draw : later)
                for (int mask = 1; mask < 32; mask++)
                    subsets.put(key(draw.draw().whites(), mask), null);
        }

        void accept(Candidate candidate) {
            total++;
            if (preview.size() < LIMIT) preview.add(candidate);
            if (sink != null) sink.accept(candidate);
            if (subsets.isEmpty()) return;
            Match match = new Match(total, candidate.whites());
            for (int mask = 0; mask < 32; mask++) {
                long key = key(candidate.whites(), mask);
                if (subsets.containsKey(key) && subsets.get(key) == null) subsets.put(key, match);
            }
        }

        Backtest compare(List<LaterDraw> later, int cutoff) {
            List<Comparison> perDraw = new ArrayList<>(), exact = new ArrayList<>();
            int best = 0;
            for (LaterDraw row : later) {
                Match nearest = null;
                int matched = -1;
                for (int mask = 0; mask < 32; mask++) {
                    Match match = subsets.get(key(row.draw().whites(), mask));
                    int size = Integer.bitCount(mask);
                    if (match != null
                            && (size > matched
                                    || size == matched && match.ordinal() < nearest.ordinal())) {
                        nearest = match;
                        matched = size;
                    }
                }
                if (nearest == null) continue;
                var candidate = nearest.whites();
                var shared = row.draw().whites().stream().filter(candidate::contains).toList();
                var comparison =
                        new Comparison(
                                row.drawNumber(),
                                row.draw().date(),
                                row.drawNumber() - cutoff,
                                row.draw().whites(),
                                candidate,
                                shared,
                                shared.size());
                perDraw.add(comparison);
                if (shared.size() == 5) exact.add(comparison);
                best = Math.max(best, shared.size());
            }
            final int highest = best;
            return new Backtest(
                    later.size(),
                    exact.size(),
                    best,
                    List.copyOf(exact),
                    perDraw.stream().filter(row -> row.matchedBalls() == highest).toList(),
                    List.copyOf(perDraw));
        }
    }

    private static long key(List<Integer> whites, int mask) {
        long key = 0;
        for (int i = 0; i < 5; i++) if ((mask & (1 << i)) != 0) key = (key << 7) | whites.get(i);
        return key;
    }

    public Result generate(List<DrawRecord> draws, String game) {
        return generate(draws, game, List.of(), 0, null);
    }

    public Result generate(
            List<DrawRecord> draws,
            String game,
            List<LaterDraw> later,
            int cutoff,
            Consumer<Candidate> sink) {
        if (later.stream().anyMatch(row -> row.drawNumber() <= cutoff))
            throw new IllegalArgumentException("Comparison draws must be after the cutoff");
        int maximum = DrawRecord.maximum(game);
        if (draws.isEmpty())
            throw new IllegalArgumentException("Select at least one training draw");
        Map<Integer, Integer> sums = new TreeMap<>(), deviations = new TreeMap<>();
        for (DrawRecord draw : draws) {
            sums.merge(draw.sum(), 1, Integer::sum);
            deviations.merge((int) Math.floor(draw.deviation()), 1, Integer::sum);
        }
        var sumModes = modes(sums);
        var deviationModes = modes(deviations);
        Set<Integer> allowedDeviations = new HashSet<>();
        deviationModes.forEach(mode -> allowedDeviations.add(mode.value()));
        Set<String> unseen = new HashSet<>();
        new RangePatternAnalyzer()
                .compare(draws, game).stream()
                        .filter(row -> ((Number) row.get("observed")).intValue() == 0)
                        .forEach(row -> unseen.add((String) row.get("pattern")));
        Collector candidates = new Collector(later, sink);
        for (Mode mode : sumModes) {
            search(
                    new ArrayList<>(),
                    1,
                    maximum,
                    mode.value(),
                    mode.value(),
                    allowedDeviations,
                    unseen,
                    candidates);
        }
        return new Result(
                draws.size(),
                sumModes,
                deviationModes,
                unseen.size(),
                List.copyOf(candidates.preview),
                candidates.total > LIMIT,
                candidates.total,
                candidates.compare(later, cutoff));
    }

    private List<Mode> modes(Map<Integer, Integer> counts) {
        int highest = Collections.max(counts.values());
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() == highest)
                .map(entry -> new Mode(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void search(
            List<Integer> chosen,
            int start,
            int maximum,
            int remaining,
            int sum,
            Set<Integer> deviations,
            Set<String> unseen,
            Collector result) {
        if (unseen.isEmpty()) return;
        int left = 5 - chosen.size();
        if (left == 0) {
            if (remaining != 0) return;
            double mean = sum / 5.0;
            double deviation =
                    Math.sqrt(chosen.stream().mapToDouble(n -> (n - mean) * (n - mean)).sum() / 5);
            int floor = (int) Math.floor(deviation);
            if (!deviations.contains(floor)) return;
            int[] buckets = new int[maximum / 10 + 1];
            chosen.forEach(n -> buckets[n / 10]++);
            String pattern =
                    String.join("-", Arrays.stream(buckets).mapToObj(Integer::toString).toList());
            if (unseen.contains(pattern))
                result.accept(new Candidate(List.copyOf(chosen), sum, deviation, floor, pattern));
            return;
        }
        if (start + left - 1 > maximum
                || remaining < left * start + left * (left - 1) / 2
                || remaining > left * maximum - left * (left - 1) / 2) return;
        int low = left == 1 ? remaining : start;
        int high = left == 1 ? remaining : maximum - left + 1;
        for (int n = low; n <= high; n++) {
            chosen.add(n);
            search(chosen, n + 1, maximum, remaining - n, sum, deviations, unseen, result);
            chosen.remove(chosen.size() - 1);
        }
    }
}
