package com.vipin.lottery.web;

import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.*;
import java.util.*;

@Component
public class HistoryParser {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public List<DrawRecord> parse(String text, String game) {
        int max = DrawRecord.maximum(game);
        if (text == null || text.length() > 3_000_000) throw new IllegalArgumentException("History must be at most 3 MB");
        String[] lines = text.replaceFirst("^\uFEFF", "").split("\\R");
        TreeMap<LocalDate, DrawRecord> records = new TreeMap<>();
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            try {
                String[] fields = lines[i].trim().split("\\s+");
                if (fields.length != 6 && fields.length != 7) throw new IllegalArgumentException("Expected date, five white balls, and optional special ball");
                LocalDate date = LocalDate.parse(fields[0], DATE);
                if (date.isAfter(LocalDate.now(ZoneId.of("America/New_York")))) throw new IllegalArgumentException("Future date");
                List<Integer> balls = new ArrayList<>();
                for (int n = 1; n <= 5; n++) balls.add(Integer.parseInt(fields[n]));
                DrawRecord row = new DrawRecord(date, balls, fields.length == 7 ? Integer.valueOf(fields[6]) : null);
                if (row.whites().get(4) > max) throw new IllegalArgumentException("White ball exceeds " + max);
                DrawRecord old = records.putIfAbsent(date, row);
                if (old != null && !old.equals(row)) throw new IllegalArgumentException("Conflicting duplicate date " + date);
            } catch (RuntimeException e) { throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage()); }
        }
        if (records.isEmpty()) throw new IllegalArgumentException("The history file is empty");
        if (records.size() > 30000) throw new IllegalArgumentException("At most 30,000 draws per import");
        return List.copyOf(records.values());
    }
}
