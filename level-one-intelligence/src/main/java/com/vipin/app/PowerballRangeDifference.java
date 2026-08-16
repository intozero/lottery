package com.vipin.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Prints range combinations that exist in the complete Powerball range file,
 * do not exist in the current range file, and contain exactly one count of 2
 * and exactly three counts of 1.
 */
public final class PowerballRangeDifference {

    private PowerballRangeDifference() {
    }

    public static void main(String[] args) {
        Path allRanges = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("files/pb-all-range-combination.txt");
        Path currentRanges = args.length > 1
                ? Paths.get(args[1])
                : Paths.get("files/pb-current-range-combination.txt");

        try {
            Set<String> all = readNormalizedLines(allRanges);
            Set<String> current = readNormalizedLines(currentRanges);

            all.removeAll(current);

            int printed = 0;
            for (String line : all) {
                if (hasRequiredCounts(line)) {
                    System.out.println(line);
                    printed++;
                }
            }

            System.out.printf("%nPrinted %d matching lines from %d difference lines.%n",
                    printed, all.size());
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Unable to compare range files: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static Set<String> readNormalizedLines(Path path) throws IOException {
        Set<String> lines = new LinkedHashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = normalize(line);
                if (!normalized.isEmpty()) {
                    lines.add(normalized);
                }
            }
        }
        return lines;
    }

    private static String normalize(String line) {
        return line.trim().replaceAll("\\s+", " ");
    }

    private static boolean hasRequiredCounts(String line) {
        String[] fields = line.split(" ");
        if (fields.length != 16) {
            throw new IllegalArgumentException("invalid range line: " + line);
        }

        int numberOfTwos = 0;
        int numberOfOnes = 0;
        for (int index = 1; index < fields.length; index += 2) {
            final int count;
            try {
                count = Integer.parseInt(fields[index]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("invalid range count in line: " + line);
            }
            if (count == 2) {
                numberOfTwos++;
            } else if (count == 1) {
                numberOfOnes++;
            }
        }
        return numberOfTwos == 1 && numberOfOnes == 3;
    }
}
