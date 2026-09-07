package totsincecombined;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Console entry point for combined total, since, and number-range analysis. */
public final class AllNumbers {
    private AllNumbers() { }

    // Inputs are always read from the console; saved IDE arguments are ignored.
    public static void main(String[] args) {
        int status = run(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out, System.err);
        if (status != 0) System.exit(status);
    }

    static int run(BufferedReader console, PrintStream out, PrintStream err) {
        try {
            String game = prompt(console, out, "Lottery PB or MM", "PB").toUpperCase(Locale.ROOT);
            if (!game.equals("PB") && !game.equals("MM")) throw new IllegalArgumentException("Choose PB or MM; F5 was an unimplemented placeholder");
            int maximum = game.equals("PB") ? 69 : 75;
            String actionText = prompt(console, out, "Action LAST, SIM, NUM_OCCUR, RAN", "LAST").toUpperCase(Locale.ROOT);
            ReportWriter.Action action;
            try { action = ReportWriter.Action.valueOf(actionText); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Unknown action: " + actionText); }
            String defaultInput = game.equals("PB") ? "files/pb/pb-sorted.txt" : "files/archive/mm-sorted.txt";
            Path input = Paths.get(prompt(console, out, "Input file", defaultInput));
            Path output = Paths.get("totsincecombined/target/" + game.toLowerCase(Locale.ROOT) + "-" + action.name().toLowerCase(Locale.ROOT) + ".txt");
            List<Draw> draws = new DrawReader().read(input, maximum);
            writeReport(input, output, draws, maximum, action);
            out.println();
            try (BufferedReader report = Files.newBufferedReader(output, StandardCharsets.UTF_8)) {
                String line;
                while ((line = report.readLine()) != null) out.println(line);
            }
            out.println("Report: " + output.toAbsolutePath().normalize());
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            err.println("totsincecombined: " + e.getMessage());
            return 1;
        }
    }

    private static String prompt(BufferedReader console, PrintStream out, String label, String defaultValue) throws IOException {
        out.print(label + " [" + defaultValue + "]: ");
        out.flush();
        String value = console.readLine();
        if (value == null) throw new IOException("Console input ended while reading " + label);
        return value.trim().isEmpty() ? defaultValue : value.trim();
    }

    static void writeReport(Path input, Path output, List<Draw> draws, int maximum, ReportWriter.Action action) throws IOException {
        if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                || (Files.exists(output) && Files.isSameFile(input, output)))
            throw new IOException("Report path must differ from input");
        Path absolute = output.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temp = Files.createTempFile(absolute.getParent(), ".totsince-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                new ReportWriter().write(draws, maximum, action, writer);
            }
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
