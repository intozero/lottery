package com.vipin.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares purchased Powerball numbers with historical draws.
 *
 * <p>Purchased-file format: five white balls followed by one Powerball.
 * Historical-file format: date, five white balls, and one Powerball.</p>
 */
public final class PowerballMatchComparator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final int TOP_MATCHES = 10;

    private PowerballMatchComparator() {
    }

    public static void main(String[] args) {
        Path purchasedFile = args.length > 0
                ? Paths.get(args[0]) : Paths.get("files/pb-bought.txt");
        Path historicalFile = args.length > 1
                ? Paths.get(args[1]) : Paths.get("files/pb-sorted.txt");

        try {
            List<Ticket> tickets = readTickets(purchasedFile);
            List<Draw> draws = readDraws(historicalFile);

            System.out.printf("Comparing %d purchased lines against %d historical draws.%n%n",
                    tickets.size(), draws.size());

            for (int index = 0; index < tickets.size(); index++) {
                printTopMatches(index + 1, tickets.get(index), draws);
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Unable to compare Powerball files: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static List<Ticket> readTickets(Path path) throws IOException {
        List<Ticket> tickets = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.trim().split("\\s+");
                if (fields.length != 6) {
                    throw formatError(path, lineNumber, "expected 6 numbers");
                }
                tickets.add(new Ticket(parseWhiteBalls(fields, 0, path, lineNumber),
                        parseNumber(fields[5], path, lineNumber)));
            }
        }
        return tickets;
    }

    private static List<Draw> readDraws(Path path) throws IOException {
        List<Draw> draws = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.trim().split("\\s+");
                if (fields.length != 6 && fields.length != 7) {
                    throw formatError(path, lineNumber,
                            "expected a date, 5 white balls, and an optional Powerball");
                }
                try {
                    LocalDate date = LocalDate.parse(fields[0], DATE_FORMAT);
                    draws.add(new Draw(date, parseWhiteBalls(fields, 1, path, lineNumber),
                            fields.length == 7
                                    ? parseNumber(fields[6], path, lineNumber) : null));
                } catch (RuntimeException exception) {
                    throw formatError(path, lineNumber, exception.getMessage());
                }
            }
        }
        return draws;
    }

    private static Set<Integer> parseWhiteBalls(
            String[] fields, int offset, Path path, int lineNumber) {
        Set<Integer> whiteBalls = new HashSet<>();
        for (int index = offset; index < offset + 5; index++) {
            whiteBalls.add(parseNumber(fields[index], path, lineNumber));
        }
        if (whiteBalls.size() != 5) {
            throw formatError(path, lineNumber, "group 1 must contain 5 distinct numbers");
        }
        return whiteBalls;
    }

    private static int parseNumber(String value, Path path, int lineNumber) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw formatError(path, lineNumber, "invalid number: " + value);
        }
    }

    private static IllegalArgumentException formatError(Path path, int line, String message) {
        return new IllegalArgumentException(path + ":" + line + ": " + message);
    }

    private static void printTopMatches(int ticketNumber, Ticket ticket, List<Draw> draws) {
        List<Match> matches = new ArrayList<>();
        for (Draw draw : draws) {
            Set<Integer> matchingWhiteBalls = new HashSet<>(ticket.whiteBalls);
            matchingWhiteBalls.retainAll(draw.whiteBalls());
            matches.add(new Match(draw, matchingWhiteBalls,
                    draw.powerball() == null ? null : ticket.powerball.equals(draw.powerball())));
        }

        matches.sort(Comparator.comparingInt(Match::totalMatches).reversed()
                .thenComparing(Comparator.comparingInt(Match::whiteMatchCount).reversed())
                .thenComparing(Match::powerballSortValue, Comparator.reverseOrder())
                .thenComparing(match -> match.draw.date, Comparator.reverseOrder()));

        System.out.printf("Purchased line %d: group 1 %s | group 2 [%d]%n",
                ticketNumber, sortedNumbers(ticket.whiteBalls), ticket.powerball);
        System.out.println("Rank  Date        Group 1  Group 2  Total  "
                + "Matching group 1 numbers  Actual group 1 numbers");

        int resultCount = Math.min(TOP_MATCHES, matches.size());
        for (int index = 0; index < resultCount; index++) {
            Match match = matches.get(index);
            System.out.printf("%4d  %-10s  %d/5      %-3s      %d      %-25s %s%n",
                    index + 1,
                    match.draw.date.format(DATE_FORMAT),
                    match.whiteMatchCount(),
                    match.powerballDisplay(),
                    match.totalMatches(),
                    sortedNumbers(match.matchingWhiteBalls),
                    sortedNumbers(match.draw.whiteBalls()));
        }
        System.out.println();
    }

    private static List<Integer> sortedNumbers(Set<Integer> numbers) {
        List<Integer> sorted = new ArrayList<>(numbers);
        sorted.sort(Integer::compareTo);
        return sorted;
    }

    private static class Ticket {
        private final Set<Integer> whiteBalls;
        private final Integer powerball;

        private Ticket(Set<Integer> whiteBalls, Integer powerball) {
            this.whiteBalls = whiteBalls;
            this.powerball = powerball;
        }

        final Set<Integer> whiteBalls() {
            return whiteBalls;
        }

        final Integer powerball() {
            return powerball;
        }
    }

    private static final class Draw extends Ticket {
        private final LocalDate date;

        private Draw(LocalDate date, Set<Integer> whiteBalls, Integer powerball) {
            super(whiteBalls, powerball);
            this.date = date;
        }
    }

    private static final class Match {
        private final Draw draw;
        private final Set<Integer> matchingWhiteBalls;
        private final Boolean powerballMatch;

        private Match(Draw draw, Set<Integer> matchingWhiteBalls, Boolean powerballMatch) {
            this.draw = draw;
            this.matchingWhiteBalls = matchingWhiteBalls;
            this.powerballMatch = powerballMatch;
        }

        private int whiteMatchCount() {
            return matchingWhiteBalls.size();
        }

        private int powerballSortValue() {
            return Boolean.TRUE.equals(powerballMatch) ? 1 : 0;
        }

        private String powerballDisplay() {
            if (powerballMatch == null) {
                return "N/A";
            }
            return powerballMatch ? "1/1" : "0/1";
        }

        private int totalMatches() {
            return whiteMatchCount() + (Boolean.TRUE.equals(powerballMatch) ? 1 : 0);
        }
    }
}
