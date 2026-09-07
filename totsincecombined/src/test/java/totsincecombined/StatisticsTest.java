package totsincecombined;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class StatisticsTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private Draw draw(int day, int... balls) { return new Draw(LocalDate.of(2026, 1, day), balls); }
    private List<Draw> history() {
        return Arrays.asList(draw(1,1,2,3,4,5), draw(2,2,6,7,8,9), draw(3,1,2,10,11,12));
    }

    @Test public void totalSinceAndGapsUseDrawIndices() {
        Statistics stats = new Statistics(69);
        for (Draw draw : history()) stats.accept(draw);
        NumberStats one = stats.numbers().get(0);
        assertEquals(2, one.getTotal()); assertEquals(0, one.getSince());
        assertEquals(Integer.valueOf(2), one.getMinGap()); assertEquals(Integer.valueOf(2), one.getMaxGap());
        assertEquals(Integer.valueOf(1), stats.numbers().get(1).getMinGap());
        assertEquals(2, stats.numbers().get(2).getSince());
        NumberStats never = stats.numbers().get(68);
        assertEquals(0, never.getTotal()); assertEquals(3, never.getSince()); assertNull(never.getLastDate());
        assertNull(never.getMinGap());
        assertEquals(15, stats.numbers().stream().mapToInt(NumberStats::getTotal).sum());
    }

    @Test public void firstAppearanceDoesNotCreateAGap() {
        Statistics stats = new Statistics(69);
        stats.accept(history().get(0));
        for (int i = 0; i < 5; i++) { assertEquals(0, stats.numbers().get(i).getSince()); assertNull(stats.numbers().get(i).getMinGap()); }
    }

    @Test public void stateAndSnapshotsAreIndependent() {
        Statistics a = new Statistics(69), b = new Statistics(69);
        a.accept(history().get(0)); List<NumberStats> before = a.numbers();
        a.accept(history().get(1));
        assertEquals(1, before.get(1).getTotal()); assertEquals(0, b.getDrawCount());
        assertThrows(UnsupportedOperationException.class, () -> a.patterns().put("bad", 2));
        int[] balls = {1,2,3,4,5}; Draw draw = draw(4, balls); balls[0] = 69;
        assertEquals(1, draw.getWhiteBalls()[0]); draw.getWhiteBalls()[0] = 68; assertEquals(1, draw.getWhiteBalls()[0]);
    }

    @Test public void rangesNeverCollideAndShapesCountDraws() {
        Statistics stats = new Statistics(69);
        stats.accept(draw(1,1,2,3,4,5)); stats.accept(draw(2,10,20,30,40,60));
        assertEquals(Integer.valueOf(1), stats.occupancies().get("1-9: 5 balls"));
        assertEquals(Integer.valueOf(1), stats.occupancies().get("60-69: 1 balls"));
        assertEquals(Integer.valueOf(1), stats.shapes().get("5"));
        assertEquals(Integer.valueOf(1), stats.shapes().get("1+1+1+1+1"));
        assertEquals(2, stats.patterns().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, stats.occupancies().values().stream().mapToInt(Integer::intValue).sum());
        Statistics mm = new Statistics(75); mm.accept(draw(1,9,10,69,70,75));
        assertEquals(Integer.valueOf(1), mm.occupancies().get("70-75: 2 balls"));
    }

    @Test public void invalidDrawDoesNotMutateAccumulator() {
        Statistics stats = new Statistics(69); stats.accept(history().get(0));
        assertThrows(IllegalArgumentException.class, () -> stats.accept(history().get(0)));
        assertThrows(IllegalArgumentException.class, () -> stats.accept(draw(2,1,2,3,4,70)));
        assertEquals(1, stats.getDrawCount());
        assertThrows(IllegalArgumentException.class, () -> draw(2,1,1,3,4,5));
    }

    @Test public void parserSortsWhitespaceBomAndFinalLineWithoutNewline() throws Exception {
        Path input = file("draws.txt", "\uFEFF1/2/2026\t1 2 3 4 5 1\r\n\r\n1/1/2026  6 7 8 9 10");
        List<Draw> draws = new DrawReader().read(input,69);
        assertEquals(2, draws.size()); assertEquals(LocalDate.of(2026,1,1), draws.get(0).getDate());
        assertArrayEquals(new int[]{1,2,3,4,5}, draws.get(1).getWhiteBalls());
    }

    @Test public void parserRejectsMalformedAndDuplicateInput() throws Exception {
        String[] invalid = {"2/30/2026 1 2 3 4 5", "1/1/2026 1 2 3 4", "1/1/2026 1 1 3 4 5",
                "1/1/2026 1 2 3 4 70", "1/1/2026 1 2 3 4 5 0",
                "1/1/2026 1 2 3 4 5\n1/1/2026 6 7 8 9 10"};
        for (String value : invalid) {
            IOException e = assertThrows(IOException.class, () -> new DrawReader().read(file("bad.txt",value),69));
            assertTrue(e.getMessage().contains("line "));
        }
        assertThrows(IOException.class, () -> new DrawReader().read(file("empty.txt","\n"),69));
    }

    @Test public void everyModeHasExpectedSnapshotsAndDeterministicOutput() throws Exception {
        for (ReportWriter.Action action : ReportWriter.Action.values()) {
            String report = report(history(),action);
            assertEquals(action == ReportWriter.Action.LAST ? 1 : 3, report.split("=== Draw ",-1).length-1);
            assertTrue(report.contains("=== Draw 3 | 2026-01-03"));
            List<Draw> reversed = new ArrayList<>(history()); Collections.reverse(reversed);
            assertEquals(report, report(reversed,action));
        }
        assertTrue(report(history(),ReportWriter.Action.LAST).contains("1-9\t12\t10\n"));
        assertFalse(report(history(),ReportWriter.Action.NUM_OCCUR).contains("Number\tTotal\tSince"));
        assertTrue(report(history(),ReportWriter.Action.RAN).contains("5\t2\t66.67"));
    }

    @Test public void atomicReportPreservesInputAndPreviousOutputOnFailure() throws Exception {
        Path input = file("input.txt","original"), output = file("report.txt","previous");
        AllNumbers.writeReport(input,output,history(),69,ReportWriter.Action.LAST);
        byte[] report = Files.readAllBytes(output);
        AllNumbers.writeReport(input,output,history(),69,ReportWriter.Action.LAST);
        assertArrayEquals(report,Files.readAllBytes(output));
        assertThrows(IllegalArgumentException.class, () -> AllNumbers.writeReport(input,output,
                Arrays.asList(history().get(0),history().get(0)),69,ReportWriter.Action.LAST));
        assertArrayEquals(report,Files.readAllBytes(output));
        assertThrows(IOException.class, () -> AllNumbers.writeReport(input,input,history(),69,ReportWriter.Action.LAST));
        assertEquals("original",new String(Files.readAllBytes(input),StandardCharsets.UTF_8));
    }

    @Test public void consoleErrorsExitCleanly() throws Exception {
        try (PrintStream out = new PrintStream(new ByteArrayOutputStream())) {
            for (String input : Arrays.asList("", "F5\n", "PB\nBAD\n", "PB\nLAST\nmissing-history.txt\n"))
                assertEquals(1,AllNumbers.run(new BufferedReader(new StringReader(input)),out,out));

        }
    }

    private Path file(String name,String value) throws IOException {
        Path path = temp.getRoot().toPath().resolve(name); Files.write(path,value.getBytes(StandardCharsets.UTF_8)); return path;
    }
    private String report(List<Draw> draws,ReportWriter.Action action) throws IOException {
        StringWriter out = new StringWriter(); new ReportWriter().write(draws,69,action,out); return out.toString();
    }
}
