package com.vipin.lottery.core;

import static org.junit.jupiter.api.Assertions.*;

import com.vipin.lottery.core.analysis.NextDrawCandidates;
import com.vipin.lottery.core.io.HistoryParser;
import com.vipin.lottery.core.model.DrawRecord;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class NextDrawCandidatesTest {
    private final NextDrawCandidates search = new NextDrawCandidates();
    private final HistoryParser parser = new HistoryParser();

    @Test
    void candidatesMeetAllConstraintsAndTheLimitIsReported() {
        var draws = parser.parse("1/1/2026 5 15 25 45 60 1\n1/2/2026 5 15 25 45 60 2", "PB");
        var result = search.generate(draws, "PB");
        assertEquals(2, result.trainingCount());
        assertEquals(List.of(new NextDrawCandidates.Mode(150, 2)), result.sumModes());
        assertEquals(200, result.rows().size());
        assertTrue(result.truncated());
        Set<List<Integer>> unique = new HashSet<>();
        int floor = (int) Math.floor(draws.get(0).deviation());
        for (var row : result.rows()) {
            assertTrue(unique.add(row.whites()));
            assertEquals(5, new HashSet<>(row.whites()).size());
            assertEquals(row.whites().stream().sorted().toList(), row.whites());
            assertTrue(row.whites().get(0) >= 1 && row.whites().get(4) <= 69);
            var draw = new DrawRecord(LocalDate.of(2026, 1, 3), row.whites(), null);
            assertEquals(150, draw.sum());
            assertEquals(draw.deviation(), row.deviation());
            assertEquals(floor, (int) Math.floor(draw.deviation()));
            assertNotEquals("1-1-1-0-1-0-1", row.pattern());
            int[] buckets = new int[7];
            row.whites().forEach(n -> buckets[n / 10]++);
            assertEquals(
                    String.join("-", Arrays.stream(buckets).mapToObj(Integer::toString).toList()),
                    row.pattern());
        }
        assertEquals(result, search.generate(draws, "PB"));
    }

    @Test
    void completeSearchChecksCandidatesBeyondThePreviewAndFindsAllLaterMatches() {
        var training = parser.parse("1/1/2026 5 15 25 45 60", "PB");
        List<NextDrawCandidates.Candidate> all = new ArrayList<>();
        var baseline = search.generate(training, "PB", List.of(), 1, all::add);
        assertTrue(all.size() > 200);
        assertEquals(all.size(), baseline.totalCandidates());
        var beyondPreview = all.get(all.size() - 1).whites();
        assertFalse(baseline.rows().stream().anyMatch(row -> row.whites().equals(beyondPreview)));
        var changed = new ArrayList<>(beyondPreview);
        changed.remove(4);
        for (int n = 1; n <= 69; n++)
            if (!beyondPreview.contains(n)) {
                changed.add(n);
                break;
            }
        var later =
                List.of(
                        new NextDrawCandidates.LaterDraw(
                                2, new DrawRecord(LocalDate.of(2026, 1, 2), beyondPreview, 99)),
                        new NextDrawCandidates.LaterDraw(
                                3, new DrawRecord(LocalDate.of(2026, 1, 3), changed, 1)),
                        new NextDrawCandidates.LaterDraw(
                                4, new DrawRecord(LocalDate.of(2026, 1, 4), beyondPreview, 2)));
        var result = search.generate(training, "PB", later, 1, null);
        assertEquals(baseline.rows(), result.rows());
        assertEquals(baseline.totalCandidates(), result.totalCandidates());
        assertEquals(3, result.backtest().checkedDraws());
        assertEquals(3, result.backtest().perDraw().size());
        assertEquals(2, result.backtest().exactMatches());
        assertEquals(
                List.of(2, 4),
                result.backtest().exact().stream()
                        .map(NextDrawCandidates.Comparison::drawNumber)
                        .toList());
        assertEquals(
                List.of(1, 3),
                result.backtest().exact().stream()
                        .map(NextDrawCandidates.Comparison::drawsAfterCutoff)
                        .toList());
        for (int i = 0; i < later.size(); i++) {
            var actual = later.get(i).draw().whites();
            int best =
                    all.stream()
                            .mapToInt(
                                    row ->
                                            (int)
                                                    row.whites().stream()
                                                            .filter(actual::contains)
                                                            .count())
                            .max()
                            .orElseThrow();
            var first =
                    all.stream()
                            .filter(
                                    row ->
                                            row.whites().stream().filter(actual::contains).count()
                                                    == best)
                            .findFirst()
                            .orElseThrow();
            assertEquals(best, result.backtest().perDraw().get(i).matchedBalls());
            assertEquals(first.whites(), result.backtest().perDraw().get(i).candidate());
        }
        assertEquals(0, baseline.backtest().checkedDraws());
        var noCandidates =
                search.generate(parser.parse("1/1/2026 1 2 3 4 5", "PB"), "PB", later, 1, null);
        assertEquals(0, noCandidates.totalCandidates());
        assertEquals(3, noCandidates.backtest().checkedDraws());
        assertTrue(noCandidates.backtest().perDraw().isEmpty());
    }

    @Test
    void tiesAndNoMatchesAreExplicitWithoutRelaxingConstraints() {
        var draws = parser.parse("1/1/2026 1 2 3 4 5\n1/2/2026 1 2 3 4 6", "PB");
        var result = search.generate(draws, "PB");
        assertEquals(
                List.of(new NextDrawCandidates.Mode(15, 1), new NextDrawCandidates.Mode(16, 1)),
                result.sumModes());
        assertEquals(List.of(new NextDrawCandidates.Mode(1, 2)), result.deviationModes());
        assertTrue(result.rows().isEmpty());
        assertFalse(result.truncated());
        var tied =
                search.generate(
                        parser.parse("1/1/2026 1 2 3 4 5\n1/2/2026 5 15 25 45 60", "PB"), "PB");
        assertEquals(2, tied.deviationModes().size());
        assertThrows(IllegalArgumentException.class, () -> search.generate(List.of(), "PB"));
        assertThrows(IllegalArgumentException.class, () -> search.generate(draws, "invalid"));
    }

    @Test
    void megaMillionsUsesItsFullAnalysisUniverse() {
        var result = search.generate(parser.parse("1/1/2026 51 60 65 70 75", "MM"), "MM");
        for (var row : result.rows()) {
            assertTrue(row.whites().get(4) <= 75);
            assertEquals(8, row.pattern().split("-").length);
        }
    }
}
