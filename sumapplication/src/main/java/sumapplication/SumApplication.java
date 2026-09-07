package sumapplication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Console entry point; saved IDE program arguments are intentionally ignored. */
public final class SumApplication {
    private SumApplication() { }

    public static void main(String[] args) {
        int status = run(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), System.out, System.err);
        if (status != 0) System.exit(status);
    }

    static int run(BufferedReader console, PrintStream out, PrintStream err) {
        try {
            String game = prompt(console, out, "Lottery PB or MM", "PB").toUpperCase(Locale.ROOT);
            if (!game.equals("PB") && !game.equals("MM")) throw new IllegalArgumentException("Choose PB or MM");
            Path input = Paths.get(prompt(console, out, "Input file",
                    game.equals("PB") ? "files/pb/pb-sorted.txt" : "files/archive/mm-sorted.txt"));
            List<LineDTO> draws = new FileRead().read(input, game.equals("PB") ? 69 : 75);
            Path output = Paths.get("sumapplication/target/" + game.toLowerCase(Locale.ROOT) + "-sums.txt");
            writeReport(input, output, draws);
            out.println();
            try (BufferedReader report = Files.newBufferedReader(output, StandardCharsets.UTF_8)) {
                String line;
                while ((line = report.readLine()) != null) out.println(line);
            }
            out.println("Report: " + output.toAbsolutePath().normalize());
            return 0;
        } catch (IOException | IllegalArgumentException e) {
            err.println("sumapplication: " + e.getMessage());
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

    static void writeReport(Path input, Path output, List<LineDTO> draws) throws IOException {
        if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize())
                || (Files.exists(output) && Files.isSameFile(input, output)))
            throw new IOException("Report path must differ from input");
        Path absolute = output.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), ".sumapplication-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                new SumReport().write(draws, writer);
            }
            Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
