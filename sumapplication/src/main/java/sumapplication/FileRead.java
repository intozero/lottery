package sumapplication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.*;
import java.util.*;

/** Reads once, validates rows, and sorts by draw date. */
public final class FileRead {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public List<LineDTO> read(Path path, int maximum) throws IOException {
        List<LineDTO> draws = new ArrayList<>();
        Set<LocalDate> dates = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            int lineNumber = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.trim().isEmpty()) continue;
                try {
                    String[] fields = line.trim().split("\\s+");
                    if (fields.length != 6 && fields.length != 7)
                        throw new IllegalArgumentException("Expected date, five white balls, and optional special ball");
                    if (!fields[0].matches("\\d{1,2}/\\d{1,2}/\\d{4}"))
                        throw new IllegalArgumentException("Expected M/d/yyyy date");
                    LocalDate date = LocalDate.parse(fields[0], DATE);
                    if (!dates.add(date)) throw new IllegalArgumentException("Duplicate date " + date);
                    int[] balls = new int[5];
                    for (int i = 0; i < 5; i++) balls[i] = ball(fields[i + 1], maximum);
                    if (fields.length == 7) ball(fields[6], 99); // Structural check only; not counted.
                    draws.add(new LineDTO(date, balls));
                } catch (RuntimeException e) {
                    throw new IOException(path + ": line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        if (draws.isEmpty()) throw new IOException("No draws in " + path);
        draws.sort(Comparator.comparing(LineDTO::getDate));
        return draws;
    }

    private static int ball(String token, int maximum) {
        if (!token.matches("[0-9]{1,2}")) throw new IllegalArgumentException("Invalid ball: " + token);
        int value = Integer.parseInt(token);
        if (value < 1 || value > maximum) throw new IllegalArgumentException("Ball outside 1-" + maximum + ": " + token);
        return value;
    }
}
