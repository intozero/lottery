package asnumber;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Command-line digit-pattern analysis of dated lottery draws. */
public final class AsNumber {
    private AsNumber() { }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) System.exit(status);
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        Path input = Paths.get("files/pb/pb-sorted.txt");
        Path output = Paths.get("as-number/target/as-number-report.txt");
        try {
            if (args.length == 1 && "--help".equals(args[0])) {
                out.println("Usage: java -jar as-number/target/as-number-1.0-SNAPSHOT.jar");
                out.println("Input: files/pb/pb-sorted.txt; report: as-number/target/as-number-report.txt.");
                return 0;
            }
            if (args.length != 0) throw new IllegalArgumentException("Run without arguments; enter the window in the console");
            out.print("Enter window length: ");
            out.flush();
            String value = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
            if (value == null) throw new IllegalArgumentException("No window value received from console");
            int window;
            try {
                window = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Window must be a positive integer, e.g. 10");
            }
            if (window < 1) throw new IllegalArgumentException("Window must be positive");
            if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                    || (Files.exists(output) && Files.isSameFile(input, output)))
                throw new IllegalArgumentException("Output must differ from input");
            List<LineDto> draws = new FileRead().read(input);
            String report = analyze(draws, window);
            writeReport(output, report);
            out.println("Analyzed " + draws.size() + " draws, " + draws.get(0).getDate() + " through "
                    + draws.get(draws.size() - 1).getDate() + "; substring length " + window + ".");
            out.println("Report: " + output.toAbsolutePath().normalize());
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            err.println("as-number: " + e.getMessage());
            return 1;
        }
    }

    static String analyze(List<LineDto> input, int window) {
        if (input.isEmpty()) throw new IllegalArgumentException("No draws to analyze");
        List<LineDto> draws = new ArrayList<>(input);
        draws.sort(Comparator.comparing(LineDto::getDate));
        StringBuilder digits = new StringBuilder();
        Map<String, List<LocalDate>> combinations = new TreeMap<>();
        Set<LocalDate> dates = new HashSet<>();
        for (LineDto draw : draws) {
            if (!dates.add(draw.getDate())) throw new IllegalArgumentException("Duplicate draw date " + draw.getDate());
            digits.append(draw.digits());
            combinations.computeIfAbsent(draw.combination(), key -> new ArrayList<>()).add(draw.getDate());
        }
        Map<String, Integer> counts = countSubstrings(digits.toString(), window);
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
        StringBuilder report = new StringBuilder("Lottery digit-pattern analysis\n");
        report.append("Draws: ").append(draws.size()).append("\nDates: ").append(draws.get(0).getDate())
                .append(" through ").append(draws.get(draws.size() - 1).getDate())
                .append("\nDigits: ").append(digits.length()).append("\nWindow length: ").append(window)
                .append("\nWindows counted: ").append(digits.length() - window + 1)
                .append("\nUnique substrings: ").append(counts.size())
                .append("\nCounting: overlapping windows, including across draw boundaries; unpadded numbers.\n")
                .append("\nRepeated combinations (five sorted white balls, then special ball):\n");
        int repeated = 0;
        for (Map.Entry<String, List<LocalDate>> entry : combinations.entrySet()) {
            if (entry.getValue().size() > 1) {
                repeated++;
                report.append(entry.getKey()).append("\t").append(entry.getValue()).append('\n');
            }
        }
        if (repeated == 0) report.append("None\n");
        report.append("\nSubstring\tCount (highest first; ties by digits)\n");
        for (Map.Entry<String, Integer> entry : sorted) report.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
        return report.toString();
    }

    static Map<String, Integer> countSubstrings(String digits, int window) {
        if (window < 1 || window > digits.length())
            throw new IllegalArgumentException("Window must be between 1 and digit-stream length " + digits.length());
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i <= digits.length() - window; i++) counts.merge(digits.substring(i, i + window), 1, Integer::sum);
        return counts;
    }

    private static void writeReport(Path output, String report) throws IOException {
        Path absolute = output.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), ".as-number-", ".tmp");
        try {
            Files.write(temporary, report.getBytes(StandardCharsets.UTF_8));
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
