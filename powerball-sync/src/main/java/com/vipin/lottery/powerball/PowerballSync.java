package com.vipin.lottery.powerball;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Dependency-free Java 8+ validator and updater for the modern Powerball matrix. */
public final class PowerballSync {
    public static final String SOURCE = "https://www.texaslottery.com/export/sites/lottery/Games/Powerball/Winning_Numbers/powerball.csv";
    static final LocalDate MATRIX_START = LocalDate.of(2015, 10, 7);
    static final LocalDate MONDAY_START = LocalDate.of(2021, 8, 23);
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private PowerballSync() { }

    public static void main(String[] args) {
        try {
            boolean write = false;
            Path file = Paths.get("files/pb_visual/pb-sorted.txt");
            Path sourceFile = null;
            for (int i = 0; i < args.length; i++) {
                if ("--write".equals(args[i])) write = true;
                else if ("--file".equals(args[i]) && i + 1 < args.length) file = Paths.get(args[++i]);
                else if ("--source-csv".equals(args[i]) && i + 1 < args.length) sourceFile = Paths.get(args[++i]);
                else throw new IllegalArgumentException("Usage: PowerballSync [--write] [--file PATH] [--source-csv PATH]");
            }
            LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
            byte[] original = Files.readAllBytes(file);
            List<String> csv = sourceFile == null ? download() : Files.readAllLines(sourceFile, StandardCharsets.UTF_8);
            Result result = reconcile(new String(original, StandardCharsets.UTF_8), csv, today);
            Path report = file.resolveSibling(file.getFileName() + ".audit.txt");
            String summary = result.summary();
            Files.write(report, ("Source: " + SOURCE + "\nRetrieved/checked: " + Instant.now()
                    + "\nInput: " + (sourceFile == null ? "live HTTPS" : sourceFile)
                    + "\n" + summary + "\n" + String.join("\n", result.issues) + "\n").getBytes(StandardCharsets.UTF_8));
            System.out.println(summary);
            System.out.println("Audit: " + report);
            byte[] replacement = result.output.getBytes(StandardCharsets.UTF_8);
            if (Arrays.equals(original, replacement)) {
                System.out.println("Already up to date; no changes.");
            } else if (write) {
                Path backup = replace(file, original, replacement);
                System.out.println("Updated " + file + "; backup: " + backup);
            } else {
                System.out.println("Check only. Run with --write to apply repairs and missing draws.");
            }
        } catch (Exception e) {
            System.err.println("Powerball sync failed: " + e.getMessage());
            System.exit(1);
        }
    }

    static List<String> download() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SOURCE).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("Accept", "text/csv");
        connection.setRequestProperty("User-Agent", "lottery-powerball-sync/1.0");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Source HTTP " + connection.getResponseCode());
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                    if (lines.size() > 50000) throw new IOException("Source exceeds safety limit");
                }
            }
            return lines;
        } finally {
            connection.disconnect();
        }
    }

    static Result reconcile(String original, List<String> csv, LocalDate today) {
        if (csv.isEmpty() || csv.size() > 50000)
            throw new IllegalArgumentException("Empty or oversized source CSV");
        TreeMap<LocalDate, int[]> official = new TreeMap<>();
        for (int i = 0; i < csv.size(); i++) {
            String[] fields = csv.get(i).split(",", -1);
            if ((fields.length != 10 && fields.length != 11) || !"Powerball".equals(fields[0]))
                throw new IllegalArgumentException("Invalid source row " + (i + 1));
            LocalDate date = LocalDate.of(Integer.parseInt(fields[3]),
                    Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
            if (date.isBefore(MATRIX_START)) continue;
            if (date.isAfter(today) || !isDrawDate(date)) throw new IllegalArgumentException("Invalid source date " + date);
            int[] balls = parseBalls(String.join(" ", Arrays.copyOfRange(fields, 4, 10)));
            if (official.put(date, balls) != null) throw new IllegalArgumentException("Duplicate source date " + date);
        }
        if (official.isEmpty()) throw new IllegalArgumentException("No supported draws in source");
        // Allow today's draw to be unpublished, but never silently accept a stale feed.
        LocalDate required = today.minusDays(1);
        while (!isDrawDate(required)) required = required.minusDays(1);
        if (official.lastKey().isBefore(required)) throw new IllegalArgumentException("Source is stale: latest "
                + official.lastKey() + ", expected at least " + required + "; retry after results are published");

        Result result = new Result();
        TreeSet<LocalDate> seen = new TreeSet<>();
        Set<LocalDate> allDates = new HashSet<>();
        String[] lines = original.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            result.rows++;
            String[] fields = line.split("\\s+", 2);
            LocalDate date;
            try { date = LocalDate.parse(fields[0], DATE); }
            catch (DateTimeParseException e) { throw new IllegalArgumentException("Cannot identify date at input line " + (i + 1)); }
            if (date.isBefore(MATRIX_START) || date.isAfter(official.lastKey()))
                throw new IllegalArgumentException("Input date outside verified source range: " + date);
            if (!allDates.add(date)) {
                result.duplicates++;
                result.issues.add("DUPLICATE line " + (i + 1) + ": " + line);
            }
            if (!isDrawDate(date)) {
                result.invalidDates++;
                result.issues.add("REMOVE non-draw date at line " + (i + 1) + ": " + line);
                continue;
            }
            int[] expected = official.get(date);
            if (expected == null) throw new IllegalArgumentException("No official draw for input line " + (i + 1)
                    + " (" + date + "); file left unchanged");
            seen.add(date);
            try {
                if (fields.length != 2 || !Arrays.equals(parseBalls(fields[1]), expected))
                    throw new IllegalArgumentException("Numbers do not match");
            } catch (IllegalArgumentException e) {
                result.corrected++;
                result.issues.add("CORRECT line " + (i + 1) + ": " + line + " -> " + format(date, expected));
            }
        }
        if (seen.isEmpty()) throw new IllegalArgumentException("Input is empty; cannot infer start date");
        result.first = seen.first();
        result.last = official.lastKey();
        String newline = original.contains("\r\n") ? "\r\n" : "\n";
        List<String> output = new ArrayList<>();
        for (LocalDate date = result.first; !date.isAfter(result.last); date = date.plusDays(1)) {
            if (!isDrawDate(date)) continue;
            int[] balls = official.get(date);
            if (balls == null) throw new IllegalArgumentException("Source has a gap at " + date + "; file left unchanged");
            String line = format(date, balls);
            output.add(line);
            if (!seen.contains(date)) {
                result.added++;
                result.issues.add("ADD " + line);
            }
        }
        result.total = output.size();
        result.output = String.join(newline, output) + (original.endsWith("\n") ? newline : "");
        return result;
    }

    static int[] parseBalls(String value) {
        String[] tokens = value.trim().split("\\s+");
        if (tokens.length != 6) throw new IllegalArgumentException("Expected five white balls and one red ball");
        int[] balls = new int[6];
        for (int i = 0; i < 6; i++) {
            if (!tokens[i].matches("[0-9]{1,2}")) throw new IllegalArgumentException("Invalid ball");
            balls[i] = Integer.parseInt(tokens[i]);
            if (balls[i] < 1 || balls[i] > (i == 5 ? 26 : 69)) throw new IllegalArgumentException("Ball out of range");
        }
        Arrays.sort(balls, 0, 5);
        for (int i = 1; i < 5; i++) if (balls[i] == balls[i - 1]) throw new IllegalArgumentException("Repeated white ball");
        return balls;
    }

    static boolean isDrawDate(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.WEDNESDAY || day == DayOfWeek.SATURDAY
                || (day == DayOfWeek.MONDAY && !date.isBefore(MONDAY_START));
    }

    static String format(LocalDate date, int[] balls) {
        StringBuilder line = new StringBuilder(DATE.format(date));
        for (int ball : balls) line.append("  ").append(ball);
        return line.toString();
    }

    static Path replace(Path file, byte[] original, byte[] replacement) throws IOException {
        Path absolute = file.toAbsolutePath();
        Path temp = Files.createTempFile(absolute.getParent(), ".powerball-", ".tmp");
        try {
            Files.write(temp, replacement);
            if (!Arrays.equals(original, Files.readAllBytes(absolute))) throw new IOException("Input changed during validation; retry");
            Path backup = Files.createTempFile(absolute.getParent(), file.getFileName() + ".", ".bak");
            Files.write(backup, original);
            // No non-atomic fallback: a failed replacement must leave the original intact.
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return backup;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    static final class Result {
        int rows, duplicates, corrected, invalidDates, added, total;
        LocalDate first, last;
        String output;
        final List<String> issues = new ArrayList<>();
        String summary() {
            return "Checked " + rows + " input rows; duplicate dates: " + duplicates + "; incorrect rows: "
                    + corrected + "; non-draw date rows: " + invalidDates + "; missing draws: " + added + ".\nValidated history: " + total
                    + " draws, " + first + " through " + last + ".";
        }
    }
}
