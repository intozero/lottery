package totsincecombined;

import java.io.*;
import java.util.*;

/** Streams deterministic reports without retaining every historical snapshot. */
public final class ReportWriter {
    public enum Action { LAST, SIM, NUM_OCCUR, RAN }

    public void write(List<Draw> input, int maximum, Action action, Writer output) throws IOException {
        if (input.isEmpty()) throw new IllegalArgumentException("No draws to analyze");
        List<Draw> draws = new ArrayList<>(input);
        draws.sort(Comparator.comparing(Draw::getDate));
        Statistics stats = new Statistics(maximum);
        output.write("White-ball total and since analysis\nMode: " + action + "\nInput draws: " + draws.size()
                + "\nDates: " + draws.get(0).getDate() + " through " + draws.get(draws.size() - 1).getDate()
                + "\nWhite-ball universe: 1-" + maximum
                + "\nSince = completed draws after last appearance; never seen = snapshot draw count."
                + "\nGaps = distance between successive appearance draw indices; NA = fewer than two appearances."
                + "\nSpecial balls excluded. History is analyzed as supplied, without official-results verification.\n");
        for (Draw draw : draws) {
            stats.accept(draw);
            if (action != Action.LAST) snapshot(stats, action, output);
        }
        if (action == Action.LAST) snapshot(stats, action, output);
    }

    private void snapshot(Statistics stats, Action action, Writer out) throws IOException {
        out.write("\n=== Draw " + stats.getDrawCount() + " | " + stats.getLatest() + " ===\n");
        if (action == Action.RAN) {
            List<String> labels = new ArrayList<>();
            for (int i = 0; i <= stats.getMaximum() / 10; i++) labels.add(stats.rangeLabel(i));
            out.write("Pattern bucket order: " + String.join(", ", labels) + "\n");
            frequencies("Range patterns", stats.patterns(), stats.getDrawCount(), out);
            frequencies("Occupancy shapes (e.g. 3+2)", stats.shapes(), stats.getDrawCount(), out);
            long occupied = stats.occupancies().values().stream().mapToLong(Integer::longValue).sum();
            frequencies("Occupied range/count categories", stats.occupancies(), occupied, out);
            return;
        }
        List<NumberStats> numbers = new ArrayList<>(stats.numbers());
        if (action == Action.LAST) {
            numbers.sort(Comparator.comparingInt(NumberStats::getTotal).thenComparingInt(NumberStats::getNumber));
            numberTable("Numbers by total ascending", numbers, out);
            numbers.sort(Comparator.comparingInt(NumberStats::getSince).reversed().thenComparingInt(NumberStats::getNumber));
            numberTable("Numbers by since descending", numbers, out);
        } else if (action == Action.SIM) {
            numberTable("Numbers in numerical order", numbers, out);
        }
        out.write("Range\tTotal appearances\tSum of since\n");
        for (int bucket = 0; bucket <= stats.getMaximum() / 10; bucket++) {
            long total = 0, since = 0;
            for (NumberStats number : numbers) {
                if (number.getNumber() / 10 == bucket) { total += number.getTotal(); since += number.getSince(); }
            }
            out.write(stats.rangeLabel(bucket) + "\t" + total + "\t" + since + "\n");
        }
    }

    private void numberTable(String heading, List<NumberStats> numbers, Writer out) throws IOException {
        out.write(heading + "\nNumber\tTotal\tSince\tLast date\tMin gap\tMax gap\n");
        for (NumberStats n : numbers) {
            out.write(n.getNumber() + "\t" + n.getTotal() + "\t" + n.getSince() + "\t"
                    + (n.getLastDate() == null ? "NEVER" : n.getLastDate()) + "\t"
                    + (n.getMinGap() == null ? "NA" : n.getMinGap()) + "\t"
                    + (n.getMaxGap() == null ? "NA" : n.getMaxGap()) + "\n");
        }
    }

    private void frequencies(String heading, Map<String, Integer> values, long denominator, Writer out) throws IOException {
        out.write(heading + " (percent denominator: " + denominator + ")\nCategory\tCount\tPercent\n");
        List<Map.Entry<String, Integer>> rows = new ArrayList<>(values.entrySet());
        rows.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        for (Map.Entry<String, Integer> row : rows)
            out.write(row.getKey() + "\t" + row.getValue() + "\t" + String.format(Locale.ROOT, "%.2f", 100.0 * row.getValue() / denominator) + "\n");
    }
}
