package com.vipin.lottery.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vipin.lottery.core.analysis.AnalysisService;
import com.vipin.lottery.core.io.HistoryParser;
import com.vipin.lottery.core.model.DrawRecord;
import com.vipin.lottery.web.persistence.HistoryStore;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:webtest;DB_CLOSE_DELAY=-1",
            "lottery.seed=false"
        })
@AutoConfigureMockMvc
class WebIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HistoryStore store;
    @Autowired HistoryParser parser;
    @Autowired AnalysisService analysis;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void clean() {
        db.update("DELETE FROM draw_changes");
        db.update("DELETE FROM imports");
        db.update("DELETE FROM draws");
    }

    List<DrawRecord> fixture() {
        return parser.parse("1/1/2026 1 2 3 4 5 1\n1/2/2026 1 2 3 4 6 2", "PB");
    }

    @Test
    void importsAreIdempotentAndScopedByGame() {
        assertEquals(2, store.save("PB", fixture(), "test", false).added());
        assertEquals(2, store.save("PB", fixture(), "test", false).unchanged());
        assertEquals(2, store.save("MM", fixture(), "test", false).added());
        assertEquals(2, store.all("PB").size());
        assertEquals(3, store.imports().size());
    }

    @Test
    void conflictingImportRollsBackEverything() {
        store.save("PB", fixture(), "original", false);
        var conflict = parser.parse("1/3/2026 10 11 12 13 14 1\n1/1/2026 10 11 12 13 14 1", "PB");
        assertThrows(
                IllegalArgumentException.class,
                () -> store.save("PB", List.of(conflict.get(1), conflict.get(0)), "bad", false));
        assertEquals(2, store.all("PB").size());
        assertEquals(1, store.imports().size());
    }

    @Test
    void correctionsRetainPreviousValues() {
        store.save("PB", fixture(), "initial", false);
        var corrected = parser.parse("1/1/2026 10 11 12 13 14 1", "PB");
        assertEquals(1, store.save("PB", corrected, "official", true).corrected());
        assertEquals("1 2 3 4 5 1", store.changes().get(0).get("PREVIOUS_VALUES"));
        assertEquals(corrected.get(0), store.all("PB").get(0));
    }

    @Test
    void importsFillMissingSpecialButCannotReplaceKnown() {
        var missing = parser.parse("1/1/2026 1 2 3 4 5", "PB");
        store.save("PB", missing, "initial", false);
        assertEquals(1, store.save("PB", fixture(), "fill", false).corrected());
        assertThrows(
                IllegalArgumentException.class,
                () -> store.save("PB", parser.parse("1/1/2026 1 2 3 4 5 9", "PB"), "bad", false));
    }

    @Test
    void realApisReturnHistoryStatisticsAndDownloads() throws Exception {
        store.save("PB", fixture(), "test", false);
        mvc.perform(get("/api/analysis").param("game", "PB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.averageSum").value(15.5))
                .andExpect(jsonPath("$.sums[1].runningAverage").value(15.5))
                .andExpect(jsonPath("$.numbers[4].since").value(1))
                .andExpect(jsonPath("$.numbers[68].since").value(2));
        mvc.perform(get("/api/history").param("game", "PB").param("from", "2026-01-02"))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/export").param("game", "PB"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        "1/1/2026  1  2  3  4  5  1\n1/2/2026  1  2  3  4  6  2\n"));
        for (String report : List.of("sums", "last", "sim", "num_occur", "ran"))
            mvc.perform(get("/api/export").param("game", "PB").param("report", report))
                    .andExpect(status().isOk());
        mvc.perform(get("/api/history").param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rowStepSelectsSourceRowsBeforeDatesAndPreservesStoredHistory() throws Exception {
        var rows = new ArrayList<DrawRecord>();
        for (int day = 1; day <= 11; day++)
            rows.add(
                    new DrawRecord(
                            java.time.LocalDate.of(2026, 1, day),
                            List.of(1, 2, 3, 4, day + 4),
                            day));
        store.save("PB", rows, "test", false);
        assertEquals(rows, store.selected("PB", null, null, 1));
        assertEquals(
                List.of(rows.get(0), rows.get(5), rows.get(10)),
                store.selected("PB", null, null, 5));
        assertEquals(List.of(rows.get(0)), store.selected("PB", null, null, Integer.MAX_VALUE));
        assertEquals(List.of(), store.selected("MM", null, null, 2));
        mvc.perform(
                        get("/api/history")
                                .param("rowStep", "2")
                                .param("from", "2026-01-02")
                                .param("to", "2026-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].date").value("2026-01-03"))
                .andExpect(jsonPath("$[0].drawNumber").value(3))
                .andExpect(jsonPath("$[2].drawNumber").value(7))
                .andExpect(jsonPath("$[2].date").value("2026-01-07"));
        mvc.perform(get("/api/history").param("game", "PB"))
                .andExpect(jsonPath("$[0].drawNumber").value(1))
                .andExpect(jsonPath("$[10].drawNumber").value(11));
        mvc.perform(
                        get("/api/history")
                                .param("game", "PB")
                                .param("startDraw", "2")
                                .param("rowStep", "5"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].drawNumber").value(2))
                .andExpect(jsonPath("$[1].drawNumber").value(7));
        mvc.perform(
                        get("/api/history")
                                .param("game", "PB")
                                .param("startDraw", "2")
                                .param("rowStep", "5")
                                .param("from", "2026-01-03"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].drawNumber").value(7));
        mvc.perform(get("/api/history").param("game", "PB").param("startDraw", "2147483647"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/history-bounds").param("game", "PB"))
                .andExpect(jsonPath("$.lastDraw").value(11));
        mvc.perform(get("/api/history-bounds").param("game", "MM"))
                .andExpect(jsonPath("$.lastDraw").value(0));
        mvc.perform(
                        get("/api/history")
                                .param("startDraw", "2")
                                .param("lastDraw", "7")
                                .param("rowStep", "5"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].drawNumber").value(7));
        mvc.perform(
                        get("/api/history")
                                .param("startDraw", "2")
                                .param("lastDraw", "6")
                                .param("rowStep", "5"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].drawNumber").value(2));
        mvc.perform(get("/api/history").param("startDraw", "5").param("lastDraw", "5"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].drawNumber").value(5));
        mvc.perform(get("/api/history").param("startDraw", "5").param("lastDraw", "4"))
                .andExpect(status().isBadRequest());
        assertEquals(rows, store.all("PB"));
    }

    @Test
    void everyHistoryResultAndExportUsesTheSameSteppedRows() throws Exception {
        var rows = new ArrayList<DrawRecord>();
        for (int day = 1; day <= 11; day++)
            rows.add(
                    new DrawRecord(
                            java.time.LocalDate.of(2026, 1, day),
                            List.of(1, 2, 3, 4, day + 4),
                            day));
        var endpoints =
                List.of(
                        "analysis",
                        "timeline",
                        "digits",
                        "ranges",
                        "export?report=history",
                        "export?report=sums",
                        "export?report=last",
                        "export?report=sim",
                        "export?report=num_occur",
                        "export?report=ran");
        for (int startDraw : List.of(1, 2, 3))
            for (int step : List.of(2, 5)) {
                clean();
                store.save("PB", rows, "test", false);
                var actual = new ArrayList<String>();
                for (String endpoint : endpoints)
                    actual.add(
                            mvc.perform(
                                            get("/api/" + endpoint)
                                                    .param("game", "PB")
                                                    .param("rowStep", String.valueOf(step))
                                                    .param("startDraw", String.valueOf(startDraw))
                                                    .param("lastDraw", "8")
                                                    .param("number", "5")
                                                    .param("window", "2"))
                                    .andExpect(status().isOk())
                                    .andReturn()
                                    .getResponse()
                                    .getContentAsString());
                var selected = store.selected("PB", null, null, step, startDraw, 8);
                clean();
                store.save("PB", selected, "test", false);
                for (int i = 0; i < endpoints.size(); i++)
                    mvc.perform(
                                    get("/api/" + endpoints.get(i))
                                            .param("game", "PB")
                                            .param("number", "5")
                                            .param("window", "2"))
                            .andExpect(status().isOk())
                            .andExpect(content().string(actual.get(i)));
            }
    }

    @Test
    void invalidRowStepsAreRejectedOnEveryHistoryEndpoint() throws Exception {
        for (String endpoint :
                List.of("history", "analysis", "timeline", "digits", "ranges", "export"))
            for (String parameter : List.of("rowStep", "startDraw", "lastDraw"))
                for (String step : List.of("0", "-1", "1.5", "abc", "2147483648"))
                    mvc.perform(
                                    get("/api/" + endpoint)
                                            .param("game", "PB")
                                            .param(parameter, step)
                                            .param("number", "1")
                                            .param("window", "2"))
                            .andExpect(status().isBadRequest());
    }

    @Test
    void nextDrawCandidatesNeverTrainOnTheTargetOrLaterDraws() throws Exception {
        store.save("PB", fixture(), "test", false);
        var request = get("/api/next-draw-candidates").param("game", "PB").param("lastDraw", "1");
        String before =
                mvc.perform(request)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.targetDraw").value(2))
                        .andExpect(jsonPath("$.result.trainingCount").value(1))
                        .andExpect(jsonPath("$.result.sumModes[0].value").value(15))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        store.save("PB", parser.parse("1/3/2026 5 15 25 45 60", "PB"), "later", false);
        String after =
                mvc.perform(
                                get("/api/next-draw-candidates")
                                        .param("game", "PB")
                                        .param("lastDraw", "1"))
                        .andExpect(jsonPath("$.result.backtest.checkedDraws").value(2))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var beforeTree = mapper.readTree(before);
        var afterTree = mapper.readTree(after);
        ((com.fasterxml.jackson.databind.node.ObjectNode) beforeTree.get("result"))
                .remove("backtest");
        ((com.fasterxml.jackson.databind.node.ObjectNode) afterTree.get("result"))
                .remove("backtest");
        assertEquals(beforeTree, afterTree);
        mvc.perform(
                        get("/api/next-draw-candidates")
                                .param("lastDraw", "3")
                                .param("startDraw", "2")
                                .param("rowStep", "2"))
                .andExpect(jsonPath("$.targetDraw").value(4))
                .andExpect(jsonPath("$.result.trainingCount").value(1))
                .andExpect(jsonPath("$.result.sumModes[0].value").value(16));
        mvc.perform(
                        get("/api/next-draw-candidates")
                                .param("lastDraw", "3")
                                .param("to", "2026-01-01"))
                .andExpect(jsonPath("$.targetDraw").value(4))
                .andExpect(jsonPath("$.result.trainingCount").value(1));
        mvc.perform(get("/api/next-draw-candidates").param("lastDraw", "4"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/next-draw-candidates").param("game", "MM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeCandidateDownloadAndLaterComparisonIgnoreTrainingDateAndStepFilters()
            throws Exception {
        var training = parser.parse("1/1/2026 5 15 25 45 60", "PB");
        var candidates =
                new ArrayList<com.vipin.lottery.core.analysis.NextDrawCandidates.Candidate>();
        new com.vipin.lottery.core.analysis.NextDrawCandidates()
                .generate(training, "PB", List.of(), 1, candidates::add);
        var exact =
                new DrawRecord(
                        java.time.LocalDate.of(2026, 1, 2),
                        candidates.get(candidates.size() - 1).whites(),
                        1);
        store.save("PB", List.of(training.get(0), exact), "test", false);
        mvc.perform(
                        get("/api/next-draw-candidates")
                                .param("lastDraw", "1")
                                .param("to", "2026-01-01")
                                .param("rowStep", "5"))
                .andExpect(jsonPath("$.result.totalCandidates").value(candidates.size()))
                .andExpect(jsonPath("$.result.backtest.checkedDraws").value(1))
                .andExpect(jsonPath("$.result.backtest.exactMatches").value(1))
                .andExpect(jsonPath("$.result.backtest.exact[0].drawNumber").value(2));
        var download =
                mvc.perform(get("/api/next-draw-candidates/export").param("lastDraw", "1"))
                        .andExpect(request().asyncStarted())
                        .andReturn();
        var content =
                mvc.perform(asyncDispatch(download))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertEquals(candidates.size() + 1, content.lines().count());
        assertTrue(
                content.contains(
                        String.join(" ", exact.whites().stream().map(Object::toString).toList())
                                + "\t"));
    }

    @Test
    void csrfIsRequiredAndMultipartWorks() throws Exception {
        String body = "{\"game\":\"PB\",\"text\":\"1/1/2026 1 2 3 4 5 1\"}";
        mvc.perform(post("/api/import-text").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/import-text")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(1));
        mvc.perform(
                        multipart("/api/import")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "history.txt",
                                                "text/plain",
                                                "1/2/2026 1 2 3 4 6 2".getBytes()))
                                .param("game", "PB")
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(1));
        mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
