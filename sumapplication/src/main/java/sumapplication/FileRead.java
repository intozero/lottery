package sumapplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Reads the repository's whitespace-delimited lottery history format. */
public final class FileRead {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/d/yyyy");

    public List<LineDTO> read(Path file) throws IOException {
        List<LineDTO> draws = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.trim().isEmpty()) {
                    draws.add(parse(line, file, lineNumber));
                }
            }
        }
        return draws;
    }

    LineDTO parse(String line, Path file, int lineNumber) {
        String[] fields = line.trim().split("\\s+");
        if (fields.length != 6 && fields.length != 7) {
            throw invalidLine(file, lineNumber,
                    "expected a date, five white balls, and an optional bonus ball");
        }

        try {
            LocalDate date = LocalDate.parse(fields[0], DATE_FORMAT);
            int[] whiteBalls = new int[5];
            for (int index = 0; index < whiteBalls.length; index++) {
                whiteBalls[index] = Integer.parseInt(fields[index + 1]);
            }
            Integer bonusBall = fields.length == 7 ? Integer.valueOf(fields[6]) : null;
            return new LineDTO(date, whiteBalls, bonusBall);
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw invalidLine(file, lineNumber, exception.getMessage());
        }
    }

    private IllegalArgumentException invalidLine(Path file, int lineNumber, String reason) {
        return new IllegalArgumentException(file + ":" + lineNumber + ": " + reason);
    }
}
