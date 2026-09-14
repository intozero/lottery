package com.vipin.lottery.web.api;

import com.vipin.lottery.core.analysis.AnalysisService;
import com.vipin.lottery.core.io.HistoryParser;
import com.vipin.lottery.core.model.DrawRecord;
import com.vipin.lottery.core.source.PowerballSource;
import com.vipin.lottery.web.persistence.HistoryStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final HistoryStore store;
    private final HistoryParser parser;
    private final AnalysisService analysis;
    private final PowerballSource official;

    public ApiController(
            HistoryStore store,
            HistoryParser parser,
            AnalysisService analysis,
            PowerballSource official) {
        this.store = store;
        this.parser = parser;
        this.analysis = analysis;
        this.official = official;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "header", token.getHeaderName());
    }

    public record HistoryDraw(
            int drawNumber, LocalDate date, List<Integer> whites, Integer special) {}

    @GetMapping("/history-bounds")
    public Map<String, Integer> historyBounds(@RequestParam(defaultValue = "PB") String game) {
        return Map.of("lastDraw", store.lastDraw(game));
    }

    @GetMapping("/history")
    public List<HistoryDraw> history(
            @RequestParam(defaultValue = "PB") String game,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw) {
        return store.selectedNumbered(game, from, to, rowStep, startDraw, lastDraw).stream()
                .map(
                        row ->
                                new HistoryDraw(
                                        row.drawNumber(),
                                        row.draw().date(),
                                        row.draw().whites(),
                                        row.draw().special()))
                .toList();
    }

    @GetMapping("/analysis")
    public Map<String, Object> analysis(
            @RequestParam(defaultValue = "PB") String game,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw) {
        return analysis.analyze(store.selected(game, from, to, rowStep, startDraw, lastDraw), game);
    }

    @GetMapping("/digits")
    public Map<String, Object> digits(
            @RequestParam String game,
            @RequestParam int window,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw) {
        return analysis.digits(
                store.selected(game, from, to, rowStep, startDraw, lastDraw), window);
    }

    @GetMapping("/timeline")
    public List<Map<String, Object>> timeline(
            @RequestParam String game,
            @RequestParam int number,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw) {
        return analysis.timeline(
                store.selected(game, from, to, rowStep, startDraw, lastDraw), number, game);
    }

    @GetMapping("/ranges")
    public List<Map<String, Object>> ranges(
            @RequestParam String game,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw) {
        return analysis.rangeUniverse(
                store.selected(game, from, to, rowStep, startDraw, lastDraw), game);
    }

    @GetMapping("/next-draw-candidates")
    public Map<String, Object> nextDrawCandidates(
            @RequestParam(defaultValue = "PB") String game,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw) {
        int available = store.lastDraw(game);
        int end = lastDraw == null ? available : lastDraw;
        if (end < 1 || end > available)
            throw new IllegalArgumentException("Choose a last draw within the available history");
        var selected = store.selected(game, from, to, rowStep, startDraw, end);
        var all = store.all(game);
        var later =
                java.util.stream.IntStream.range(end, all.size())
                        .mapToObj(
                                i ->
                                        new com.vipin.lottery.core.analysis.NextDrawCandidates
                                                .LaterDraw(i + 1, all.get(i)))
                        .toList();
        var result =
                new com.vipin.lottery.core.analysis.NextDrawCandidates()
                        .generate(selected, game, later, end, null);
        return Map.of(
                "startDraw",
                startDraw,
                "lastDraw",
                end,
                "targetDraw",
                end + 1,
                "trainingFrom",
                selected.get(0).date(),
                "trainingThrough",
                selected.get(selected.size() - 1).date(),
                "result",
                result);
    }

    @GetMapping("/next-draw-candidates/export")
    public ResponseEntity<
                    org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody>
            exportCandidates(
                    @RequestParam(defaultValue = "PB") String game,
                    @RequestParam(required = false) LocalDate from,
                    @RequestParam(required = false) LocalDate to,
                    @RequestParam(defaultValue = "1") int rowStep,
                    @RequestParam(defaultValue = "1") int startDraw,
                    @RequestParam(required = false) Integer lastDraw) {
        int available = store.lastDraw(game);
        int end = lastDraw == null ? available : lastDraw;
        if (end < 1 || end > available)
            throw new IllegalArgumentException("Choose a last draw within the available history");
        var selected = store.selected(game, from, to, rowStep, startDraw, end);
        if (selected.isEmpty())
            throw new IllegalArgumentException("Select at least one training draw");
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=next-draw-candidates.txt")
                .body(
                        output -> {
                            Writer writer =
                                    new BufferedWriter(
                                            new OutputStreamWriter(output, StandardCharsets.UTF_8));
                            writer.write(
                                    "White balls\tSum\tDeviation\tDeviation floor\tUnseen range pattern\n");
                            new com.vipin.lottery.core.analysis.NextDrawCandidates()
                                    .generate(
                                            selected,
                                            game,
                                            List.of(),
                                            end,
                                            row -> {
                                                try {
                                                    writer.write(
                                                            String.join(
                                                                            " ",
                                                                            row.whites().stream()
                                                                                    .map(
                                                                                            Object
                                                                                                    ::toString)
                                                                                    .toList())
                                                                    + "\t"
                                                                    + row.sum()
                                                                    + "\t"
                                                                    + row.deviation()
                                                                    + "\t"
                                                                    + row.deviationFloor()
                                                                    + "\t"
                                                                    + row.pattern()
                                                                    + "\n");
                                                } catch (IOException e) {
                                                    throw new UncheckedIOException(e);
                                                }
                                            });
                            writer.flush();
                        });
    }

    @GetMapping("/ml-forecast")
    public Map<String, Object> machineLearningForecast(
            @RequestParam(defaultValue = "PB") String game,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw,
            @RequestParam(defaultValue = "all") String model,
            @RequestParam(defaultValue = "1") double delta,
            @RequestParam(defaultValue = "0.7") double mlWeight,
            @RequestParam(defaultValue = "150") int trainingWindow) {
        var settings =
                new com.vipin.lottery.core.analysis.MachineLearningForecast.Settings(
                        model, delta, mlWeight, trainingWindow);
        int available = store.lastDraw(game);
        int end = lastDraw == null ? available : lastDraw;
        if (end < 1 || end > available)
            throw new IllegalArgumentException("Choose a last draw within the available history");
        var selected = store.selected(game, from, to, rowStep, startDraw, end);
        var all = store.all(game);
        var later =
                java.util.stream.IntStream.range(end, all.size())
                        .mapToObj(
                                i ->
                                        new com.vipin.lottery.core.analysis.NextDrawCandidates
                                                .LaterDraw(i + 1, all.get(i)))
                        .toList();
        var result =
                new com.vipin.lottery.core.analysis.MachineLearningForecast()
                        .forecast(selected, game, later, end, settings);
        return Map.of("targetDraw", end + 1, "lastDraw", end, "result", result);
    }

    @GetMapping("/combinations")
    public Map<String, Object> combinations(
            @RequestParam int maximum,
            @RequestParam int sum,
            @RequestParam(required = false) Integer deviation) {
        return analysis.combinations(maximum, sum, deviation);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HistoryStore.ImportResult upload(
            @RequestParam String game, @RequestParam MultipartFile file) throws IOException {
        if (file.getSize() > 3_000_000)
            throw new IllegalArgumentException("Maximum file size is 3 MB");
        return store.save(
                game,
                parser.parse(new String(file.getBytes(), StandardCharsets.UTF_8), game),
                "File upload",
                false);
    }

    public record TextImport(String game, String text) {}

    @PostMapping("/import-text")
    public HistoryStore.ImportResult text(@RequestBody TextImport request) {
        return store.save(
                request.game(),
                parser.parse(request.text(), request.game()),
                "Pasted history",
                false);
    }

    @PostMapping("/powerball/sync")
    public HistoryStore.ImportResult sync() throws IOException, InterruptedException {
        List<DrawRecord> source = official.fetch();
        List<DrawRecord> existing = store.all("PB");
        LocalDate start = existing.isEmpty() ? PowerballSource.START : existing.get(0).date();
        Set<LocalDate> officialDates = new HashSet<>();
        source.forEach(d -> officialDates.add(d.date()));
        for (DrawRecord d : existing)
            if (!officialDates.contains(d.date()))
                throw new IllegalArgumentException(
                        "Cannot verify stored date " + d.date() + "; sync made no changes");
        return store.save(
                "PB",
                source.stream().filter(d -> !d.date().isBefore(start)).toList(),
                "Texas Lottery official sync",
                true);
    }

    @GetMapping("/imports")
    public List<Map<String, Object>> imports() {
        return store.imports();
    }

    @GetMapping("/changes")
    public List<Map<String, Object>> changes() {
        return store.changes();
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam String game,
            @RequestParam(defaultValue = "history") String report,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int rowStep,
            @RequestParam(defaultValue = "1") int startDraw,
            @RequestParam(required = false) Integer lastDraw)
            throws IOException {
        var draws = store.selected(game, from, to, rowStep, startDraw, lastDraw);
        StringWriter out = new StringWriter();
        if (report.equals("history")) {
            for (DrawRecord d : draws)
                out.write(
                        HistoryParser.DATE.format(d.date())
                                + "  "
                                + d.values().replace(" ", "  ")
                                + "\n");
        } else if (report.equals("sums")) {
            new com.vipin.lottery.core.report.SumReport().write(draws, out);
        } else {
            com.vipin.lottery.core.report.CombinedReport.Action action;
            try {
                action =
                        com.vipin.lottery.core.report.CombinedReport.Action.valueOf(
                                report.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown report");
            }
            new com.vipin.lottery.core.report.CombinedReport()
                    .write(draws, DrawRecord.maximum(game), action, out);
        }
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + game.toLowerCase(Locale.ROOT)
                                + "-"
                                + report.toLowerCase(Locale.ROOT)
                                + ".txt\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(out.toString());
    }
}
