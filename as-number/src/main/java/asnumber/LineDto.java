package asnumber;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/** One dated draw: five distinct white balls followed by the special ball. */
public final class LineDto {
    private final LocalDate date;
    private final int[] numbers;

    public LineDto(LocalDate date, int[] numbers) {
        this.date = Objects.requireNonNull(date, "date");
        if (numbers.length != 6) throw new IllegalArgumentException("Expected six balls");
        this.numbers = numbers.clone();
        for (int number : this.numbers) {
            if (number < 1 || number > 99) throw new IllegalArgumentException("Balls must be between 1 and 99");
        }
        Arrays.sort(this.numbers, 0, 5);
        for (int i = 1; i < 5; i++) {
            if (this.numbers[i] == this.numbers[i - 1]) throw new IllegalArgumentException("Repeated white ball");
        }
    }

    public LocalDate getDate() { return date; }

    /** Delimiters prevent different combinations with the same digits from colliding. */
    public String combination() {
        return Arrays.stream(numbers).mapToObj(Integer::toString).collect(Collectors.joining(" "));
    }

    /** Preserve the original analysis's unpadded digit representation. */
    public String digits() {
        return Arrays.stream(numbers).mapToObj(Integer::toString).collect(Collectors.joining());
    }
}
