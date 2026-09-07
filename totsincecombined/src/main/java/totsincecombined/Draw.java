package totsincecombined;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

/** Immutable draw; special balls are deliberately outside this white-ball analysis. */
public final class Draw {
    private final LocalDate date;
    private final int[] whiteBalls;

    public Draw(LocalDate date, int[] whiteBalls) {
        this.date = Objects.requireNonNull(date, "date");
        if (whiteBalls.length != 5) throw new IllegalArgumentException("Expected five white balls");
        this.whiteBalls = whiteBalls.clone();
        Arrays.sort(this.whiteBalls);
        for (int i = 0; i < 5; i++) {
            if (this.whiteBalls[i] < 1 || this.whiteBalls[i] > 99)
                throw new IllegalArgumentException("Invalid white ball");
            if (i > 0 && this.whiteBalls[i] == this.whiteBalls[i - 1])
                throw new IllegalArgumentException("Repeated white ball");
        }
    }

    public LocalDate getDate() { return date; }
    public int[] getWhiteBalls() { return whiteBalls.clone(); }
}
