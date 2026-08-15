package com.web.ai.cgpt.extractor;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CsvWriter {

    private static final Path FILE =
            Paths.get("data/powerball_results.csv");

    public static void append(List<Draw> draws) throws Exception {

        boolean exists = Files.exists(FILE);

        try(CSVWriter writer =
                    new CSVWriter(new FileWriter(FILE.toFile(), true))) {

            if(!exists) {
                writer.writeNext(new String[]{
                        "Date","N1","N2","N3","N4","N5","PB","PP"
                });
            }

            for(Draw d : draws) {

                writer.writeNext(new String[]{

                        d.getDrawDate().toString(),
                        String.valueOf(d.getWhiteBalls().get(0)),
                        String.valueOf(d.getWhiteBalls().get(1)),
                        String.valueOf(d.getWhiteBalls().get(2)),
                        String.valueOf(d.getWhiteBalls().get(3)),
                        String.valueOf(d.getWhiteBalls().get(4)),
                        String.valueOf(d.getPowerBall()),
                        String.valueOf(d.getPowerPlay())

                });

            }

        }

    }

}
