package totsincecombined;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Incremental accumulator owned by a single analysis; no shared mutable state. */
public final class Statistics {
    private final int maximum;
    private final int[] totals, last, minGap, maxGap;
    private final LocalDate[] lastDates;
    private int drawCount;
    private LocalDate latest;
    private final Map<String, Integer> patterns = new TreeMap<>();
    private final Map<String, Integer> shapes = new TreeMap<>();
    private final Map<String, Integer> occupancies = new TreeMap<>();

    public Statistics(int maximum) {
        if (maximum < 5 || maximum > 99) throw new IllegalArgumentException("Maximum must be between 5 and 99");
        this.maximum = maximum;
        totals = new int[maximum + 1]; last = new int[maximum + 1];
        minGap = new int[maximum + 1]; maxGap = new int[maximum + 1];
        lastDates = new LocalDate[maximum + 1];
    }

    public void accept(Draw draw) {
        if (latest != null && !draw.getDate().isAfter(latest))
            throw new IllegalArgumentException("Draw dates must be unique and increasing");
        int[] balls = draw.getWhiteBalls();
        for (int ball : balls) if (ball > maximum) throw new IllegalArgumentException("White ball exceeds " + maximum);
        drawCount++;
        latest = draw.getDate();
        int[] buckets = new int[maximum / 10 + 1];
        for (int number : balls) {
            if (totals[number] > 0) {
                int gap = drawCount - last[number];
                minGap[number] = totals[number] == 1 ? gap : Math.min(minGap[number], gap);
                maxGap[number] = Math.max(maxGap[number], gap);
            }
            totals[number]++;
            last[number] = drawCount;
            lastDates[number] = latest;
            buckets[number / 10]++;
        }
        String pattern = Arrays.stream(buckets).mapToObj(Integer::toString).collect(Collectors.joining("-"));
        patterns.merge(pattern, 1, Integer::sum);
        String shape = Arrays.stream(buckets).filter(n -> n > 0).boxed()
                .sorted(Comparator.reverseOrder()).map(Object::toString).collect(Collectors.joining("+"));
        shapes.merge(shape, 1, Integer::sum);
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i] > 0) occupancies.merge(rangeLabel(i) + ": " + buckets[i] + " balls", 1, Integer::sum);
        }
    }

    public List<NumberStats> numbers() {
        List<NumberStats> rows = new ArrayList<>();
        for (int number = 1; number <= maximum; number++) {
            rows.add(new NumberStats(number, totals[number], drawCount - last[number],
                    totals[number] < 2 ? null : minGap[number], totals[number] < 2 ? null : maxGap[number], lastDates[number]));
        }
        return Collections.unmodifiableList(rows);
    }

    public Map<String, Integer> patterns() { return Collections.unmodifiableMap(new TreeMap<>(patterns)); }
    public Map<String, Integer> shapes() { return Collections.unmodifiableMap(new TreeMap<>(shapes)); }
    public Map<String, Integer> occupancies() { return Collections.unmodifiableMap(new TreeMap<>(occupancies)); }
    public int getDrawCount() { return drawCount; }
    public LocalDate getLatest() { return latest; }
    public int getMaximum() { return maximum; }
    public String rangeLabel(int bucket) { return Math.max(1, bucket * 10) + "-" + Math.min(maximum, bucket * 10 + 9); }
}
