package totsincecombined;

import java.time.LocalDate;

/** Immutable statistics at one point in history. Gaps are draw-index distances. */
public final class NumberStats {
    private final int number, total, since;
    private final Integer minGap, maxGap;
    private final LocalDate lastDate;

    NumberStats(int number, int total, int since, Integer minGap, Integer maxGap, LocalDate lastDate) {
        this.number = number;
        this.total = total;
        this.since = since;
        this.minGap = minGap;
        this.maxGap = maxGap;
        this.lastDate = lastDate;
    }
    public int getNumber() { return number; }
    public int getTotal() { return total; }
    public int getSince() { return since; }
    public Integer getMinGap() { return minGap; }
    public Integer getMaxGap() { return maxGap; }
    public LocalDate getLastDate() { return lastDate; }
}
