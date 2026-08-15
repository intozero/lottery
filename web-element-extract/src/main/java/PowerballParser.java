import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PowerballParser {

    public static void main(String[] args) throws Exception {

        File input = new File("/Users/vipinvasanthakumarpadma/Documents/Projects/JavaProjects/General/lottery/files/pb-2026-web.html");

        Document doc = Jsoup.parse(input, "UTF-8");

        Elements cards = doc.select("a.card");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

        for (Element card : cards) {

            String dateText =
                    card.select("h5.card-title").text();

            LocalDate drawDate =
                    LocalDate.parse(dateText, formatter);

            Elements balls =
                    card.select("div.white-balls div");

            List<Integer> whiteBalls = new ArrayList<>();

            for (Element ball : balls) {
                whiteBalls.add(
                        Integer.parseInt(ball.text().trim()));
            }

            int powerBall =
                    Integer.parseInt(
                            card.select("div.powerball div")
                                    .text().trim());

            String powerPlay =
                    card.select(".multiplier")
                            .text()
                            .replace("x", "");

            System.out.println(drawDate);

            System.out.println(whiteBalls);

            System.out.println("PB = " + powerBall);

            System.out.println("PP = " + powerPlay);

            System.out.println("----------------------------");
        }
    }
}
