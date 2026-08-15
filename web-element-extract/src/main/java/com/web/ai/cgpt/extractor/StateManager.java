package com.web.ai.cgpt.extractor;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Properties;

public class StateManager {

    private static final Path STATE =
            Paths.get("/Users/vipinvasanthakumarpadma/Documents/Projects/JavaProjects/General/lottery/web-element-extract/data/lastProcessed.properties");

    public static LocalDate getLastProcessed() throws Exception {

        if (!Files.exists(STATE))
            return LocalDate.MIN;

        Properties p = new Properties();

        try (InputStream in = Files.newInputStream(STATE)) {
            p.load(in);
        }

        return LocalDate.parse(p.getProperty("lastProcessedDate"));
    }

    public static void save(LocalDate date) throws Exception {

        Properties p = new Properties();

        p.setProperty("lastProcessedDate", date.toString());

        try (OutputStream out = Files.newOutputStream(STATE)) {
            p.store(out, "");
        }
    }
}
