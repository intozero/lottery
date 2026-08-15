package com.web.ai.cgpt.extractor;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        LocalDate lastProcessed =
                StateManager.getLastProcessed();

        PowerballScraper scraper =
                new PowerballScraper();

        List<Draw> draws = scraper.fetch();

        List<Draw> newDraws =
                draws.stream()
                        .filter(d -> d.getDrawDate()
                                .isAfter(lastProcessed))
                        .sorted(Comparator.comparing(Draw::getDrawDate))
                        .toList();

        if(newDraws.isEmpty()){

            System.out.println("No new draws.");
            return;
        }

        CsvWriter.append(newDraws);

        StateManager.save(
                newDraws.get(newDraws.size()-1)
                        .getDrawDate());

        System.out.println("Added "
                + newDraws.size()
                + " new draws.");

    }

}
