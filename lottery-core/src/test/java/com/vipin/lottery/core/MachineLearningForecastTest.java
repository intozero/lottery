package com.vipin.lottery.core;

import static org.junit.jupiter.api.Assertions.*;

import com.vipin.lottery.core.analysis.MachineLearningForecast;
import com.vipin.lottery.core.analysis.NextDrawCandidates;
import com.vipin.lottery.core.model.DrawRecord;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class MachineLearningForecastTest {
    private final MachineLearningForecast ml = new MachineLearningForecast();

    private List<DrawRecord> history(int maximum) {
        List<DrawRecord> rows = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            List<Integer> whites = new ArrayList<>();
            for (int j = 0; j < 5; j++) whites.add((i * 7 + j * 13) % maximum + 1);
            rows.add(new DrawRecord(LocalDate.of(2026, 1, 1).plusDays(i), whites, 1));
        }
        return rows;
    }

    @Test
    void allTenRealClassifiersTrainAndReturnValidScoresAndDistinctGuesses() {
        var result =
                ml.forecast(
                        history(69),
                        "PB",
                        List.of(),
                        30,
                        new MachineLearningForecast.Settings("all", 1, .7, 20));
        assertEquals(10, result.models().size());
        assertEquals(20, result.trainingTransitions());
        assertEquals(20 * 69, result.trainingExamples());
        assertEquals(25.0 / 69, result.randomExpectedMatches());
        for (var model : result.models()) {
            assertNull(model.error(), model.name() + ": " + model.error());
            assertEquals(5, new HashSet<>(model.whites()).size());
            assertEquals(model.whites().stream().sorted().toList(), model.whites());
            assertEquals(69, model.scores().size());
            assertNull(model.nextDrawMatches());
            assertNull(model.averageMatches());
            for (var score : model.scores()) {
                assertTrue(Double.isFinite(score.learned()));
                assertTrue(score.learned() >= 0 && score.learned() <= 1);
                assertEquals(
                        .7 * score.learned() + .3 * score.historical(), score.blended(), 1e-12);
            }
        }
    }

    @Test
    void futureDrawsOnlyAffectEvaluationNotLearningAndSettingsAreDeterministic() {
        var settings = new MachineLearningForecast.Settings("naive-bayes", 2, 1, 20);
        var before = ml.forecast(history(75), "MM", List.of(), 30, settings);
        var model = before.models().get(0);
        assertNull(model.error());
        var exact = new DrawRecord(LocalDate.of(2026, 2, 1), model.whites(), 99);
        var later =
                List.of(
                        new NextDrawCandidates.LaterDraw(31, exact),
                        new NextDrawCandidates.LaterDraw(33, exact));
        var after = ml.forecast(history(75), "MM", later, 30, settings).models().get(0);
        assertEquals(model.whites(), after.whites());
        assertEquals(model.scores(), after.scores());
        assertEquals(5, after.nextDrawMatches());
        assertEquals(2, after.exactMatches());
        assertEquals(31, after.closestDraw());
        assertEquals(5.0, after.averageMatches());
        assertEquals(
                List.of(1, 3),
                after.later().stream()
                        .map(MachineLearningForecast.Evaluation::drawsAfterCutoff)
                        .toList());
        assertEquals(75, after.scores().size());
    }

    @Test
    void deltaAndZeroMlWeightChangeTheHistoricalBlendAsDocumented() {
        List<DrawRecord> history = new ArrayList<>();
        for (int i = 0; i < 30; i++)
            history.add(
                    new DrawRecord(
                            LocalDate.of(2026, 1, 1).plusDays(i),
                            i < 20 ? List.of(1, 2, 3, 4, 5) : List.of(6, 7, 8, 9, 10),
                            null));
        var equal =
                ml.forecast(
                                history,
                                "PB",
                                List.of(),
                                30,
                                new MachineLearningForecast.Settings("naive-bayes", 0, 0, 20))
                        .models()
                        .get(0);
        var recent =
                ml.forecast(
                                history,
                                "PB",
                                List.of(),
                                30,
                                new MachineLearningForecast.Settings("naive-bayes", 6, 0, 20))
                        .models()
                        .get(0);
        assertNull(equal.error());
        assertNull(recent.error());
        assertEquals(List.of(1, 2, 3, 4, 5), equal.whites());
        assertEquals(List.of(6, 7, 8, 9, 10), recent.whites());
        for (var score : recent.scores()) assertEquals(score.historical(), score.blended());
    }

    @Test
    void invalidSettingsAndTooLittleHistoryAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineLearningForecast.Settings("bad", 0, 1, 20));
        for (double delta : new double[] {-1, 11, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new MachineLearningForecast.Settings("all", delta, 1, 20));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineLearningForecast.Settings("all", 0, 2, 20));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineLearningForecast.Settings("all", 0, 1, 301));
        var settings = new MachineLearningForecast.Settings("naive-bayes", 0, 1, 20);
        assertThrows(
                IllegalArgumentException.class,
                () -> ml.forecast(history(69).subList(0, 19), "PB", List.of(), 19, settings));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        ml.forecast(
                                history(69),
                                "PB",
                                List.of(new NextDrawCandidates.LaterDraw(30, history(69).get(0))),
                                30,
                                settings));
    }
}
