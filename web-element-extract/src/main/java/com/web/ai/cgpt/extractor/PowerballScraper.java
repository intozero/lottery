package com.web.ai.cgpt.extractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PowerballScraper {

    private static final String URL =
            "https://www.powerball.com/previous-results";

    public List<Draw> fetch() throws Exception {

        Document doc = Jsoup.connect(URL)
                .userAgent("Mozilla/5.0")
                .get();

        List<Draw> draws = new ArrayList<>();

        Elements rows = doc.select("tbody tr");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("M/d/yyyy");

        for(Element row : rows){

            String date =
                    row.select("td").get(0).text();

            LocalDate drawDate =
                    LocalDate.parse(date, formatter);

            Elements balls =
                    row.select(".white-balls .ball");

            List<Integer> nums = new ArrayList<>();

            for(Element b : balls)
                nums.add(Integer.parseInt(b.text()));

            int pb = Integer.parseInt(
                    row.select(".powerball").text());

            int pp = Integer.parseInt(
                    row.select("td").last().text());

            draws.add(new Draw(drawDate, nums, pb, pp));

        }

        return draws;

    }

}
