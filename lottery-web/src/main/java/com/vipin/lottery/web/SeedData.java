package com.vipin.lottery.web;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.*;
import org.slf4j.LoggerFactory;

@Component
public class SeedData implements ApplicationRunner {
    private final HistoryStore store;
    private final HistoryParser parser;
    @Value("${lottery.seed}") boolean enabled;
    @Value("${lottery.root}") String root;
    public SeedData(HistoryStore store, HistoryParser parser) { this.store=store; this.parser=parser; }
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        for (var entry : Map.of("PB","files/pb/pb-sorted.txt","MM","files/archive/mm-sorted.txt").entrySet()) {
            if (!store.all(entry.getKey()).isEmpty()) continue;
            Path path=Paths.get(root).resolve(entry.getValue());
            if (!Files.isRegularFile(path)) continue;
            try { store.save(entry.getKey(),parser.parse(Files.readString(path),entry.getKey()),"Initial repository history",false); }
            catch (Exception e) { LoggerFactory.getLogger(getClass()).warn("Could not seed {}: {}",entry.getKey(),e.getMessage()); }
        }
    }
}
