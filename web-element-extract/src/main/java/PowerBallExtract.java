import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PowerBallExtract {

    // URL for the previous results page
    private static final String URL = "https://www.powerball.com/previous-results?gc=powerball&sd=2024-11-13&ed=2025-04-02";

    // Date format that matches the draw date strings on the page (e.g., "Wed, Oct 23, 2024")
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);

    // Starting date for filtering (October 1, 2024)
    private static final Date START_DATE;
    static {
        try {
            START_DATE = DATE_FORMAT.parse("Wed, Oct 1, 2024");
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing start date", e);
        }
    }

    public static void main(String[] args) {
        try {
            // Fetch the HTML content of the previous results page
            Document doc = Jsoup.connect(URL).get();

            // Select all draw result elements (adjust the selector based on actual page structure)
            Elements drawElements = doc.select("div.draw-result");

            Date currentDate = new Date(); // current date for filtering

            // Loop over each draw element
            for (Element draw : drawElements) {
                // Extract the draw date (adjust the selector as needed)
                Element dateElem = draw.selectFirst("div.draw-date");
                if (dateElem == null) {
                    continue;
                }
                String drawDateStr = dateElem.text().trim();
                Date drawDate;
                try {
                    drawDate = DATE_FORMAT.parse(drawDateStr);
                } catch (ParseException e) {
                    // Skip if date parsing fails
                    continue;
                }

                // Filter out draws outside the desired date range
                if (drawDate.before(START_DATE) || drawDate.after(currentDate)) {
                    continue;
                }

                // Extract the white balls (assuming they are contained in span elements with class "white-ball")
                Elements whiteBallElements = draw.select("span.white-ball");
                StringBuilder whiteBalls = new StringBuilder();
                for (Element wb : whiteBallElements) {
                    whiteBalls.append(wb.text().trim()).append(" ");
                }

                // Extract the Powerball (assuming it's in a span with class "powerball")
                Element powerballElem = draw.selectFirst("span.powerball");
                String powerball = powerballElem != null ? powerballElem.text().trim() : "";

                // Print the result
                System.out.println("Date: " + DATE_FORMAT.format(drawDate));
                System.out.println("  White Balls: " + whiteBalls.toString().trim());
                System.out.println("  Powerball: " + powerball);
                System.out.println("-----------------------------------");
            }

        } catch (IOException e) {
            System.err.println("Error fetching the page: " + e.getMessage());
        }
    }
}

