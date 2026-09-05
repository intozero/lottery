package com.vipin.lottery.powerball;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Run with the module's test.sh; no external test dependencies required. */
public final class PowerballSyncTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        List<String> source = Arrays.asList(
                "Powerball,8,22,2026,65,57,54,31,13,23,3",
                "Powerball,8,24,2026,3,16,33,38,68,2,2",
                "Powerball,8,26,2026,12,32,45,50,58,2,2");
        LocalDate today = LocalDate.of(2026, 8, 27);
        String input = "8/26/2026  12  32  45  50  58  2\r\n"
                + "8/22/2026  13  31  54  57  65\r\n"
                + "8/22/2026  13  13  54  57  65  23\r\n"
                + "8/23/2026  1  2  3  4  5  6\r\n";
        PowerballSync.Result r = PowerballSync.reconcile(input, source, today);
        check(r.rows == 4 && r.duplicates == 1 && r.corrected == 2 && r.invalidDates == 1 && r.added == 1 && r.total == 3,
                "repair counts");
        check(r.output.equals("8/22/2026  13  31  54  57  65  23\r\n8/24/2026  3  16  33  38  68  2\r\n8/26/2026  12  32  45  50  58  2\r\n"), "format/order/colors");
        PowerballSync.Result again = PowerballSync.reconcile(r.output, source, today);
        check(again.output.equals(r.output) && again.corrected == 0 && again.added == 0 && again.duplicates == 0, "idempotence");
        check(PowerballSync.parseBalls("69 1 30 40 50 1")[5] == 1, "red may repeat a white");
        rejects(() -> PowerballSync.parseBalls("1 2 3 4 70 1"), "white range");
        rejects(() -> PowerballSync.parseBalls("1 2 3 4 5 27"), "red range");
        rejects(() -> PowerballSync.parseBalls("1 2 3 4 4 1"), "duplicate whites");
        rejects(() -> PowerballSync.reconcile("2/30/2026  1 2 3 4 5 6", source, today), "strict date");
        rejects(() -> PowerballSync.reconcile("", source, today), "empty input");
        rejects(() -> PowerballSync.reconcile(input, Arrays.asList("<html>error</html>"), today), "bad response");
        rejects(() -> PowerballSync.reconcile(input, source.subList(0, 2), today), "stale source");
        rejects(() -> PowerballSync.reconcile(input, Arrays.asList(source.get(0), source.get(2)), today), "source gap");
        List<String> duplicated = new ArrayList<>(source); duplicated.add(source.get(0));
        rejects(() -> PowerballSync.reconcile(input, duplicated, today), "source duplicate");
        rejects(() -> PowerballSync.reconcile("8/29/2026  1 2 3 4 5 6", source, today), "unverified input");
        check(!PowerballSync.isDrawDate(LocalDate.of(2021, 8, 16)) && PowerballSync.isDrawDate(LocalDate.of(2021, 8, 23)), "Monday transition");
        List<String> repeated = Arrays.asList("Powerball,8,24,2026,1,2,3,4,5,1,2", "Powerball,8,26,2026,1,2,3,4,5,1,2");
        check(PowerballSync.reconcile("8/24/2026  1 2 3 4 5 1", repeated, today).total == 2, "same combination on different dates retained");
        Path dir = Files.createTempDirectory("powerball-test-");
        Path file = dir.resolve("draws.txt");
        byte[] original = input.getBytes(StandardCharsets.UTF_8);
        byte[] replacement = r.output.getBytes(StandardCharsets.UTF_8);
        Path backup = null;
        try {
            Files.write(file, original);
            backup = PowerballSync.replace(file, original, replacement);
            check(Arrays.equals(Files.readAllBytes(backup), original), "exact backup");
            check(Arrays.equals(Files.readAllBytes(file), replacement), "replacement");
            try { PowerballSync.replace(file, original, replacement); throw new AssertionError("concurrent edit accepted"); }
            catch (java.io.IOException expected) { checks++; }
        } finally {
            if (backup != null) Files.deleteIfExists(backup);
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
        System.out.println("Passed " + checks + " checks.");
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
    private static void rejects(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label);
    }
}
