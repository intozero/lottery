package com.vipin.lottery.web;

import org.springframework.stereotype.Service;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

@Service
public class PowerballSource {
    public static final String URL="https://www.texaslottery.com/export/sites/lottery/Games/Powerball/Winning_Numbers/powerball.csv";
    static final LocalDate START=LocalDate.of(2015,10,7);
    public List<DrawRecord> fetch() throws IOException, InterruptedException {
        HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).followRedirects(HttpClient.Redirect.NORMAL).build();
        var response=client.send(HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(60))
                .header("Accept","text/csv").GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream input=response.body()) {
            if(response.statusCode()!=200) throw new IOException("Official source returned HTTP "+response.statusCode());
            byte[] bytes=input.readNBytes(3_000_001);
            if(bytes.length>3_000_000) throw new IOException("Official history exceeds size limit");
            return parse(new String(bytes,java.nio.charset.StandardCharsets.UTF_8),LocalDate.now(ZoneId.of("America/New_York")));
        }
    }
    List<DrawRecord> parse(String csv,LocalDate today) {
        TreeMap<LocalDate,DrawRecord> records=new TreeMap<>();
        for(String line:csv.split("\\R")) {
            if(line.isBlank()) continue;
            String[] f=line.split(",",-1);
            if((f.length!=10&&f.length!=11)||!f[0].equals("Powerball")) throw new IllegalArgumentException("Unexpected official CSV format");
            LocalDate date=LocalDate.of(Integer.parseInt(f[3]),Integer.parseInt(f[1]),Integer.parseInt(f[2]));
            if(date.isBefore(START)) continue;
            var whites=Arrays.stream(f,4,9).map(Integer::valueOf).toList();
            DrawRecord row=new DrawRecord(date,whites,Integer.valueOf(f[9]));
            if(date.isAfter(today)||!scheduled(date)||row.whites().get(4)>69||row.special()>26)
                throw new IllegalArgumentException("Invalid official draw "+date);
            if(records.put(date,row)!=null) throw new IllegalArgumentException("Duplicate official draw "+date);
        }
        if(records.isEmpty()) throw new IllegalArgumentException("Official history is empty");
        LocalDate expected=today.minusDays(1);
        while(!scheduled(expected)) expected=expected.minusDays(1);
        if(records.lastKey().isBefore(expected)) throw new IllegalArgumentException("Official source is not yet current. Latest: "+records.lastKey());
        for(LocalDate d=START;!d.isAfter(records.lastKey());d=d.plusDays(1))
            if(scheduled(d)&&!records.containsKey(d)) throw new IllegalArgumentException("Gap in official history: "+d);
        return List.copyOf(records.values());
    }
    static boolean scheduled(LocalDate date) {
        return date.getDayOfWeek()==DayOfWeek.WEDNESDAY||date.getDayOfWeek()==DayOfWeek.SATURDAY||
                (date.getDayOfWeek()==DayOfWeek.MONDAY&&!date.isBefore(LocalDate.of(2021,8,23)));
    }
}
