package com.vipin.lottery.core;

import static org.junit.jupiter.api.Assertions.*;

import com.vipin.lottery.core.analysis.*;
import com.vipin.lottery.core.io.HistoryParser;
import com.vipin.lottery.core.model.DrawRecord;
import com.vipin.lottery.core.report.*;
import com.vipin.lottery.core.source.PowerballSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CoreTest {
    private final HistoryParser parser = new HistoryParser();
    private final AnalysisService analysis = new AnalysisService();

    private List<DrawRecord> history() {
        return parser.parse("1/1/2026 1 2 3 4 5\n1/2/2026 2 6 7 8 9\n1/3/2026 1 2 10 11 12", "PB");
    }

    @ParameterizedTest
    @ValueSource(strings = {"last", "sim", "num_occur", "ran", "sums"})
    void textReportsAreByteForByteCompatible(String mode) throws Exception {
        var out = new StringWriter();
        if (mode.equals("sums")) new SumReport().write(history(), out);
        else
            new CombinedReport()
                    .write(
                            history(),
                            69,
                            CombinedReport.Action.valueOf(mode.toUpperCase(Locale.ROOT)),
                            out);
        try (var expected = getClass().getResourceAsStream("/golden/" + mode + ".txt")) {
            assertNotNull(expected);
            assertEquals(
                    new String(expected.readAllBytes(), StandardCharsets.UTF_8), out.toString());
        }
    }

    @Test
    void totalsRecencyGapsAndUnseenValuesArePreserved() {
        var stats = new Statistics(69);
        history().forEach(stats::accept);
        assertEquals(15, stats.numbers().stream().mapToInt(NumberStats::getTotal).sum());
        var one = stats.numbers().get(0);
        assertEquals(2, one.getTotal());
        assertEquals(0, one.getSince());
        assertEquals(2, one.getMinGap());
        assertEquals(2, one.getMaxGap());
        assertEquals(1, stats.numbers().get(1).getMinGap());
        assertEquals(2, stats.numbers().get(2).getSince());
        assertEquals(3, stats.numbers().get(68).getSince());
        assertNull(stats.numbers().get(68).getLastDate());
        assertNull(stats.numbers().get(68).getMinGap());
    }

    @Test
    void immutableModelAndSnapshotsDoNotLeakState() {
        var whites = new ArrayList<>(List.of(5, 4, 3, 2, 1));
        var draw = new DrawRecord(LocalDate.of(2026, 1, 1), whites, null);
        whites.set(0, 69);
        assertEquals(List.of(1, 2, 3, 4, 5), draw.whites());
        assertThrows(UnsupportedOperationException.class, () -> draw.whites().set(0, 69));
        var stats = new Statistics(69);
        stats.accept(draw);
        var snapshot = stats.numbers();
        stats.accept(history().get(1));
        assertEquals(1, snapshot.get(1).getTotal());
        assertEquals(2, stats.numbers().get(1).getTotal());
        assertThrows(UnsupportedOperationException.class, () -> stats.patterns().put("bad", 1));
        assertThrows(IllegalArgumentException.class, () -> stats.accept(draw));
        assertEquals(2, stats.getDrawCount());
    }

    @Test
    void rangeCategoriesRemainDistinct() {
        var stats = new Statistics(69);
        stats.accept(history().get(0));
        stats.accept(new DrawRecord(LocalDate.of(2026, 1, 4), List.of(10, 20, 30, 40, 60), null));
        assertEquals(1, stats.occupancies().get("1-9: 5 balls"));
        assertEquals(1, stats.occupancies().get("60-69: 1 balls"));
        assertEquals(2, stats.patterns().values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void sumAveragesKeepFractionalPartAndReportsStayDeterministic() throws Exception {
        var data = parser.parse("1/1/2026 1 2 3 4 5 1\n1/2/2026 1 2 3 4 6 2", "PB");
        assertEquals(15.5, analysis.analyze(data, "PB").get("averageSum"));
        var normal = new StringWriter();
        var reversed = new StringWriter();
        new SumReport().write(data, normal);
        var reverse = new ArrayList<>(data);
        Collections.reverse(reverse);
        new SumReport().write(reverse, reversed);
        assertEquals(normal.toString(), reversed.toString());
        assertTrue(normal.toString().contains("15.50"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SumReport().write(List.of(data.get(0), data.get(0)), new StringWriter()));
    }

    @Test
    void parserNormalizesWithoutInventingSpecialBalls() {
        assertEquals(
                1,
                parser.parse("\uFEFF1/1/2026\t5 4 3 2 1 1\r\n1/1/2026 1 2 3 4 5 1", "PB").size());
        assertNull(history().get(0).special());
        for (String value :
                List.of(
                        "",
                        "2/30/2026 1 2 3 4 5",
                        "1/1/2026 1 1 2 3 4",
                        "1/1/2026 1 2 3 4 70",
                        "1/1/2026 1 2 3 4 5 1\n1/1/2026 1 2 3 4 6 1"))
            assertThrows(IllegalArgumentException.class, () -> parser.parse(value, "PB"));
    }

    @Test
    void digitWindowsAndTimelineAreCorrect() {
        var data = parser.parse("1/1/2026 1 2 3 4 5 1", "PB");
        assertEquals(6, analysis.digits(data, 1).get("windows"));
        assertEquals(5, analysis.digits(data, 1).get("unique"));
        assertThrows(IllegalArgumentException.class, () -> analysis.digits(data, 0));
        assertThrows(IllegalArgumentException.class, () -> analysis.digits(history(), 1));
        assertEquals(2, analysis.timeline(history(), 5, "PB").get(2).get("since"));
    }

    @Test
    void combinationsAndRangeUniverseAreExact() {
        assertEquals(
                List.of(List.of(1, 2, 3, 4, 5)), analysis.combinations(6, 15, null).get("rows"));
        assertEquals(false, analysis.combinations(6, 15, null).get("truncated"));
        assertThrows(IllegalArgumentException.class, () -> analysis.combinations(76, 100, null));
        long possible =
                analysis.rangeUniverse(history(), "PB").stream()
                        .mapToLong(r -> ((Number) r.get("combinations")).longValue())
                        .sum();
        assertEquals(11238513L, possible);
        assertEquals(
                3,
                analysis.rangeUniverse(history(), "PB").stream()
                        .mapToInt(r -> ((Number) r.get("observed")).intValue())
                        .sum());
    }

    @Test
    void officialHistoryValidationStillRejectsStaleGappedAndDuplicateData() {
        var source = new PowerballSource();
        String csv = "Powerball,10,7,2015,18,30,40,48,52,9,2";
        assertEquals(1, source.parse(csv, LocalDate.of(2015, 10, 8)).size());
        assertThrows(
                IllegalArgumentException.class,
                () -> source.parse(csv, LocalDate.of(2015, 10, 12)));
        assertThrows(
                IllegalArgumentException.class,
                () -> source.parse(csv + "\n" + csv, LocalDate.of(2015, 10, 8)));
        assertThrows(
                IllegalArgumentException.class,
                () -> source.parse("html", LocalDate.of(2015, 10, 8)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        source.parse(
                                csv + "\nPowerball,10,14,2015,15,20,29,31,40,1,2",
                                LocalDate.of(2015, 10, 15)));
    }
}
