package sumapplication;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/** Stateless sum analysis; all averages use floating-point division. */
public final class SumReport {
    public void write(List<LineDTO> input, Writer out) throws IOException {
        if (input.isEmpty()) throw new IllegalArgumentException("No draws to analyze");
        List<LineDTO> draws = new ArrayList<>(input);
        draws.sort(Comparator.comparing(LineDTO::getDate));
        Set<java.time.LocalDate> dates = new HashSet<>();
        Map<Integer, Integer> frequencies = new TreeMap<>();
        TextTable details = new TextTable(
                new String[]{"Draw", "Date", "White balls", "Sum", "Running total", "Running average"},
                true, false, false, true, true, true);
        long total = 0;
        int count = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (LineDTO draw : draws) {
            if (!dates.add(draw.getDate())) throw new IllegalArgumentException("Duplicate draw date " + draw.getDate());
            int sum = draw.sum();
            total += sum;
            count++;
            min = Math.min(min, sum);
            max = Math.max(max, sum);
            frequencies.merge(sum, 1, Integer::sum);
            String balls = Arrays.stream(draw.getWhiteBalls()).mapToObj(Integer::toString).collect(Collectors.joining(" "));
            details.row(count, draw.getDate(), balls, sum, total, decimal((double) total / count));
        }
        out.write("WHITE-BALL SUM ANALYSIS\n\n");
        new TextTable(new String[]{"Metric", "Value"}, false, false)
                .row("Draws", count)
                .row("First draw", draws.get(0).getDate())
                .row("Last draw", draws.get(draws.size() - 1).getDate())
                .row("Total of all draw sums", total)
                .row("Average draw sum", decimal((double) total / count))
                .row("Minimum draw sum", min)
                .row("Maximum draw sum", max)
                .row("Distinct sums", frequencies.size())
                .write(out);
        out.write("Only the five white balls are summed; special balls are excluded.\n"
                + "Averages are per draw, displayed to two decimal places.\n\n"
                + "Per-draw sums (chronological order)\n");
        details.write(out);
        out.write("Sum frequencies (count ascending, ties by sum ascending)\n");
        TextTable frequencyTable = new TextTable(new String[]{"Sum", "Draw count", "Percent"}, true, true, true);
        List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(frequencies.entrySet());
        sorted.sort(Map.Entry.<Integer, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()));
        for (Map.Entry<Integer, Integer> row : sorted)
            frequencyTable.row(row.getKey(), row.getValue(), decimal(100.0 * row.getValue() / count) + "%");
        frequencyTable.write(out);
    }

    private String decimal(double value) { return String.format(Locale.ROOT, "%.2f", value); }
}
