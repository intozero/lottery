package sumapplication;

import java.time.LocalDate;
import java.util.Arrays;

/** One lottery drawing. The optional sixth number is not part of the white-ball sum. */
public final class LineDTO {
    private final LocalDate lotDate;
    private final int[] whiteBalls;
    private final Integer bonusBall;

    public LineDTO(LocalDate lotDate, int[] whiteBalls, Integer bonusBall) {
        if (lotDate == null) {
            throw new IllegalArgumentException("lotDate is required");
        }
        if (whiteBalls == null || whiteBalls.length != 5) {
            throw new IllegalArgumentException("exactly five white balls are required");
        }
        this.lotDate = lotDate;
        this.whiteBalls = Arrays.copyOf(whiteBalls, whiteBalls.length);
        this.bonusBall = bonusBall;
    }

    public LocalDate getLotDate() {
        return lotDate;
    }

    public int[] getWhiteBalls() {
        return Arrays.copyOf(whiteBalls, whiteBalls.length);
    }

    public Integer getBonusBall() {
        return bonusBall;
    }

    public int whiteBallSum() {
        int sum = 0;
        for (int number : whiteBalls) {
            sum += number;
        }
        return sum;
    }
}
