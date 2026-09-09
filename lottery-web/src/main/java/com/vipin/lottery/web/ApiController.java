package com.vipin.lottery.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final HistoryStore store;
    private final HistoryParser parser;
    private final AnalysisService analysis;
    private final PowerballSource official;
    public ApiController(HistoryStore store,HistoryParser parser,AnalysisService analysis,PowerballSource official) {
        this.store=store;this.parser=parser;this.analysis=analysis;this.official=official;
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"header",token.getHeaderName()); }
    @GetMapping("/history") public List<DrawRecord> history(@RequestParam(defaultValue="PB") String game,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) { return store.selected(game,from,to); }
    @GetMapping("/analysis") public Map<String,Object> analysis(@RequestParam(defaultValue="PB") String game,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) { return analysis.analyze(store.selected(game,from,to),game); }
    @GetMapping("/digits") public Map<String,Object> digits(@RequestParam String game,@RequestParam int window,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) { return analysis.digits(store.selected(game,from,to),window); }
    @GetMapping("/timeline") public List<Map<String,Object>> timeline(@RequestParam String game,@RequestParam int number,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) { return analysis.timeline(store.selected(game,from,to),number,game); }
    @GetMapping("/ranges") public List<Map<String,Object>> ranges(@RequestParam String game,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) { return analysis.rangeUniverse(store.selected(game,from,to),game); }
    @GetMapping("/combinations") public Map<String,Object> combinations(@RequestParam int maximum,@RequestParam int sum,
            @RequestParam(required=false) Integer deviation) { return analysis.combinations(maximum,sum,deviation); }
    @PostMapping(value="/import",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public HistoryStore.ImportResult upload(@RequestParam String game,@RequestParam MultipartFile file) throws IOException {
        if(file.getSize()>3_000_000)throw new IllegalArgumentException("Maximum file size is 3 MB");
        return store.save(game,parser.parse(new String(file.getBytes(),StandardCharsets.UTF_8),game),"File upload",false);
    }
    public record TextImport(String game,String text) {}
    @PostMapping("/import-text") public HistoryStore.ImportResult text(@RequestBody TextImport request) {
        return store.save(request.game(),parser.parse(request.text(),request.game()),"Pasted history",false);
    }
    @PostMapping("/powerball/sync") public HistoryStore.ImportResult sync() throws IOException,InterruptedException {
        List<DrawRecord> source=official.fetch();
        List<DrawRecord> existing=store.all("PB");
        LocalDate start=existing.isEmpty()?PowerballSource.START:existing.get(0).date();
        Set<LocalDate> officialDates=new HashSet<>();source.forEach(d->officialDates.add(d.date()));
        for(DrawRecord d:existing)if(!officialDates.contains(d.date()))throw new IllegalArgumentException("Cannot verify stored date "+d.date()+"; sync made no changes");
        return store.save("PB",source.stream().filter(d->!d.date().isBefore(start)).toList(),"Texas Lottery official sync",true);
    }
    @GetMapping("/imports") public List<Map<String,Object>> imports(){return store.imports();}
    @GetMapping("/changes") public List<Map<String,Object>> changes(){return store.changes();}

    @GetMapping("/export") public ResponseEntity<String> export(@RequestParam String game,@RequestParam(defaultValue="history") String report,
            @RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to) throws IOException {
        var draws=store.selected(game,from,to);
        StringWriter out=new StringWriter();
        if(report.equals("history")) {
            for(DrawRecord d:draws)out.write(HistoryParser.DATE.format(d.date())+"  "+d.values().replace(" ","  ")+"\n");
        } else if(report.equals("sums")) {
            new sumapplication.SumReport().write(draws.stream().map(d->new sumapplication.LineDTO(d.date(),d.whites().stream().mapToInt(Integer::intValue).toArray())).toList(),out);
        } else {
            totsincecombined.ReportWriter.Action action;
            try {action=totsincecombined.ReportWriter.Action.valueOf(report.toUpperCase(Locale.ROOT));}
            catch(IllegalArgumentException e){throw new IllegalArgumentException("Unknown report");}
            new totsincecombined.ReportWriter().write(draws.stream().map(d->new totsincecombined.Draw(d.date(),d.whites().stream().mapToInt(Integer::intValue).toArray())).toList(),
                    DrawRecord.maximum(game),action,out);
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+game.toLowerCase(Locale.ROOT)+"-"+report.toLowerCase(Locale.ROOT)+".txt\"")
                .contentType(new MediaType("text","plain",StandardCharsets.UTF_8)).body(out.toString());
    }
}
