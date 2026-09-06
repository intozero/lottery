package asnumber;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reads UTF-8 draw files without changing them. */
public final class FileRead {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public List<LineDto> read(Path input) throws IOException {
        List<LineDto> draws = new ArrayList<>();
        Set<LocalDate> dates = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.trim().isEmpty()) continue;
                try {
                    LineDto draw = parse(line);
                    if (!dates.add(draw.getDate())) throw new IllegalArgumentException("Duplicate draw date " + draw.getDate());
                    draws.add(draw);
                } catch (RuntimeException e) {
                    throw new IOException(input + ": line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        }
        if (draws.isEmpty()) throw new IOException("No draws found in " + input);
        draws.sort(Comparator.comparing(LineDto::getDate));
        return draws;
    }

    static LineDto parse(String line) {
        String[] fields = line.trim().split("\\s+");
        if (fields.length != 7) throw new IllegalArgumentException("Expected date and six balls (M/d/yyyy W1 W2 W3 W4 W5 RED)");
        if (!fields[0].matches("\\d{1,2}/\\d{1,2}/\\d{4}")) throw new IllegalArgumentException("Invalid date format");
        LocalDate date = LocalDate.parse(fields[0], DATE);
        int[] balls = new int[6];
        for (int i = 0; i < balls.length; i++) {
            if (!fields[i + 1].matches("[0-9]{1,2}")) throw new IllegalArgumentException("Invalid ball: " + fields[i + 1]);
            balls[i] = Integer.parseInt(fields[i + 1]);
        }
        return new LineDto(date, balls);
    }
}
