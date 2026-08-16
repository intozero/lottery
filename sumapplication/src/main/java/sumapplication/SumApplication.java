package sumapplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Calculates per-draw white-ball sums and their frequency distribution. */
public final class SumApplication {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/d/yyyy");

    private SumApplication() {
    }

    public static void main(String[] args) {
        try {
            Path input = resolveInput(args);
            List<LineDTO> draws = new FileRead().read(input);
            if (draws.isEmpty()) {
                throw new IllegalArgumentException("input file contains no drawings: " + input);
            }

            SumReport report = analyze(draws);
            printReport(input, report);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Unable to calculate lottery sums: " + exception.getMessage());
            System.exit(1);
        }
    }

    /** Pure analysis method that can be reused by tests or other modules. */
    public static SumReport analyze(List<LineDTO> draws) {
        if (draws == null) {
            throw new IllegalArgumentException("draws are required");
        }

        List<DrawSum> drawSums = new ArrayList<>();
        Map<Integer, Integer> frequencies = new LinkedHashMap<>();
        long total = 0;

        for (LineDTO draw : draws) {
            int sum = draw.whiteBallSum();
            total += sum;
            drawSums.add(new DrawSum(draw, sum, total / (drawSums.size() + 1.0)));
            frequencies.put(sum, frequencies.getOrDefault(sum, 0) + 1);
        }

        List<Map.Entry<Integer, Integer>> sortedEntries =
                new ArrayList<>(frequencies.entrySet());
        sortedEntries.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));

        Map<Integer, Integer> sortedFrequencies = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : sortedEntries) {
            sortedFrequencies.put(entry.getKey(), entry.getValue());
        }

        return new SumReport(drawSums, sortedFrequencies, total);
    }

    private static Path resolveInput(String[] args) {
        if (args.length > 1) {
            throw new IllegalArgumentException("usage: SumApplication [PB|MM|path-to-file]");
        }

        String selection;
        if (args.length == 1) {
            selection = args[0];
        } else {
            System.out.print("Enter PB, MM, or a lottery file path: ");
            Scanner scanner = new Scanner(System.in);
            if (!scanner.hasNextLine()) {
                throw new IllegalArgumentException("no lottery or file was provided");
            }
            selection = scanner.nextLine().trim();
        }

        if ("PB".equalsIgnoreCase(selection)) {
            return Paths.get("files", "pb-sorted.txt");
        }
        if ("MM".equalsIgnoreCase(selection)) {
            Path current = Paths.get("files", "mm-sorted.txt");
            return Files.exists(current)
                    ? current : Paths.get("files", "archive", "mm-sorted.txt");
        }
        if (selection.trim().isEmpty()) {
            throw new IllegalArgumentException("lottery or file path cannot be empty");
        }
        return Paths.get(selection);
    }

    private static void printReport(Path input, SumReport report) {
        System.out.println("Input: " + input);
        System.out.println("Date        Sum   Running average");
        for (DrawSum result : report.getDrawSums()) {
            System.out.printf("%-10s  %3d   %.2f%n",
                    result.getDraw().getLotDate().format(DATE_FORMAT),
                    result.getSum(), result.getRunningAverage());
        }

        System.out.println();
        System.out.println("Sum frequency (most frequent first)");
        System.out.println("Sum   Occurrences");
        for (Map.Entry<Integer, Integer> entry : report.getFrequencies().entrySet()) {
            System.out.printf("%3d   %d%n", entry.getKey(), entry.getValue());
        }

        System.out.printf("%nDraws: %d%n", report.getDrawSums().size());
        System.out.println("Total of all white-ball sums: " + report.getTotal());
        System.out.printf("Overall average: %.2f%n", report.getAverage());
    }

    public static final class DrawSum {
        private final LineDTO draw;
        private final int sum;
        private final double runningAverage;

        private DrawSum(LineDTO draw, int sum, double runningAverage) {
            this.draw = draw;
            this.sum = sum;
            this.runningAverage = runningAverage;
        }

        public LineDTO getDraw() {
            return draw;
        }

        public int getSum() {
            return sum;
        }

        public double getRunningAverage() {
            return runningAverage;
        }
    }

    public static final class SumReport {
        private final List<DrawSum> drawSums;
        private final Map<Integer, Integer> frequencies;
        private final long total;

        private SumReport(List<DrawSum> drawSums,
                          Map<Integer, Integer> frequencies, long total) {
            this.drawSums = Collections.unmodifiableList(new ArrayList<>(drawSums));
            this.frequencies = Collections.unmodifiableMap(
                    new LinkedHashMap<>(frequencies));
            this.total = total;
        }

        public List<DrawSum> getDrawSums() {
            return drawSums;
        }

        public Map<Integer, Integer> getFrequencies() {
            return frequencies;
        }

        public long getTotal() {
            return total;
        }

        public double getAverage() {
            return drawSums.isEmpty() ? 0.0 : total / (double) drawSums.size();
        }
    }
}
