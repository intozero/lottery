package com.vipin.lottery.web;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:webtest;DB_CLOSE_DELAY=-1","lottery.seed=false"})
@AutoConfigureMockMvc
class WebIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HistoryStore store;
    @Autowired HistoryParser parser;
    @Autowired AnalysisService analysis;
    @Autowired JdbcTemplate db;
    @BeforeEach void clean(){db.update("DELETE FROM draw_changes");db.update("DELETE FROM imports");db.update("DELETE FROM draws");}
    List<DrawRecord> fixture(){return parser.parse("1/1/2026 1 2 3 4 5 1\n1/2/2026 1 2 3 4 6 2","PB");}

    @Test void importsAreIdempotentAndScopedByGame(){
        assertEquals(2,store.save("PB",fixture(),"test",false).added());
        assertEquals(2,store.save("PB",fixture(),"test",false).unchanged());
        assertEquals(2,store.save("MM",fixture(),"test",false).added());
        assertEquals(2,store.all("PB").size());
        assertEquals(3,store.imports().size());
    }
    @Test void conflictingImportRollsBackEverything(){
        store.save("PB",fixture(),"original",false);
        var conflict=parser.parse("1/3/2026 10 11 12 13 14 1\n1/1/2026 10 11 12 13 14 1","PB");
        assertThrows(IllegalArgumentException.class,()->store.save("PB",List.of(conflict.get(1),conflict.get(0)),"bad",false));
        assertEquals(2,store.all("PB").size());assertEquals(1,store.imports().size());
    }
    @Test void correctionsRetainPreviousValues(){
        store.save("PB",fixture(),"initial",false);
        var corrected=parser.parse("1/1/2026 10 11 12 13 14 1","PB");
        assertEquals(1,store.save("PB",corrected,"official",true).corrected());
        assertEquals("1 2 3 4 5 1",store.changes().get(0).get("PREVIOUS_VALUES"));
        assertEquals(corrected.get(0),store.all("PB").get(0));
    }
    @Test void importsFillMissingSpecialButCannotReplaceKnown(){
        var missing=parser.parse("1/1/2026 1 2 3 4 5","PB");
        store.save("PB",missing,"initial",false);
        assertEquals(1,store.save("PB",fixture(),"fill",false).corrected());
        assertThrows(IllegalArgumentException.class,()->store.save("PB",parser.parse("1/1/2026 1 2 3 4 5 9","PB"),"bad",false));
    }
    @Test void parserValidatesAndNormalizes(){
        assertEquals(1,parser.parse("\uFEFF1/1/2026\t5 4 3 2 1 1\r\n1/1/2026 1 2 3 4 5 1","PB").size());
        for(String text:List.of("","2/30/2026 1 2 3 4 5","1/1/2026 1 1 2 3 4","1/1/2026 1 2 3 4 70",
                "1/1/2026 1 2 3 4 5 1\n1/1/2026 1 2 3 4 6 1"))
            assertThrows(IllegalArgumentException.class,()->parser.parse(text,"PB"));
    }
    @Test void realApisReturnHistoryStatisticsAndDownloads() throws Exception {
        store.save("PB",fixture(),"test",false);
        mvc.perform(get("/api/analysis").param("game","PB")).andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2)).andExpect(jsonPath("$.averageSum").value(15.5))
                .andExpect(jsonPath("$.sums[1].runningAverage").value(15.5))
                .andExpect(jsonPath("$.numbers[4].since").value(1)).andExpect(jsonPath("$.numbers[68].since").value(2));
        mvc.perform(get("/api/history").param("game","PB").param("from","2026-01-02")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/export").param("game","PB")).andExpect(status().isOk()).andExpect(content().string("1/1/2026  1  2  3  4  5  1\n1/2/2026  1  2  3  4  6  2\n"));
        for(String report:List.of("sums","last","sim","num_occur","ran"))
            mvc.perform(get("/api/export").param("game","PB").param("report",report)).andExpect(status().isOk());
        mvc.perform(get("/api/history").param("from","2026-02-01").param("to","2026-01-01")).andExpect(status().isBadRequest());
    }
    @Test void csrfIsRequiredAndMultipartWorks() throws Exception {
        String body="{\"game\":\"PB\",\"text\":\"1/1/2026 1 2 3 4 5 1\"}";
        mvc.perform(post("/api/import-text").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/import-text").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.added").value(1));
        mvc.perform(multipart("/api/import").file(new MockMultipartFile("file","history.txt","text/plain","1/2/2026 1 2 3 4 6 2".getBytes())).param("game","PB").with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.added").value(1));
        mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        mvc.perform(get("/")).andExpect(status().isOk());
    }
    @Test void digitWindowsAndTimelineAreCorrect(){
        var one=parser.parse("1/1/2026 1 2 3 4 5 1","PB");
        var result=analysis.digits(one,1);
        assertEquals(6,result.get("windows"));
        assertEquals(5,result.get("unique"));
        assertThrows(IllegalArgumentException.class,()->analysis.digits(one,0));
        assertThrows(IllegalArgumentException.class,()->analysis.digits(parser.parse("1/1/2026 1 2 3 4 5","PB"),1));
        var timeline=analysis.timeline(fixture(),5,"PB");
        assertEquals(1,timeline.get(1).get("since"));
    }
    @Test void combinationsAndRangeUniverseAreExact(){
        var all=analysis.combinations(6,15,null);
        assertEquals(List.of(List.of(1,2,3,4,5)),all.get("rows"));
        assertEquals(false,all.get("truncated"));
        assertThrows(IllegalArgumentException.class,()->analysis.combinations(76,100,null));
        long count=analysis.rangeUniverse(fixture(),"PB").stream().mapToLong(r->((Number)r.get("combinations")).longValue()).sum();
        assertEquals(11238513L,count); // C(69,5), not including the special ball
        assertEquals(2,analysis.rangeUniverse(fixture(),"PB").stream().mapToInt(r->((Number)r.get("observed")).intValue()).sum());
    }
    @Test void officialParserRejectsIncompleteAndStaleSource(){
        var source=new PowerballSource();
        String csv="Powerball,10,7,2015,18,30,40,48,52,9,2";
        assertEquals(1,source.parse(csv,LocalDate.of(2015,10,8)).size());
        assertThrows(IllegalArgumentException.class,()->source.parse(csv,LocalDate.of(2015,10,12)));
        assertThrows(IllegalArgumentException.class,()->source.parse(csv+"\n"+csv,LocalDate.of(2015,10,8)));
        assertThrows(IllegalArgumentException.class,()->source.parse("html",LocalDate.of(2015,10,8)));
    }
}
