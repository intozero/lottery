
package totsincecombined;

import groupingrange.GROUPINGRANGE;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.*;

import maxmindiffoccurence.MaxMinDiffOccurence;
import util.logic.layer.SinceOccurenceRangeTotal;
import util.logic.layer.TotalOccurenceRangeTotal;


/**
 * @author vipin
 */
public class AllNumbers {
    private static String filePath;
    public static LinkedHashMap<Integer, ArrayList> trio = new LinkedHashMap<>();

    public static void main(String[] args) throws ParseException, InterruptedException, IOException {


        System.out.println("Enter which lottery MM , PB or F5");
        Scanner scanner = new Scanner(System.in);
        String inputType = scanner.next();

        if (inputType.equalsIgnoreCase("MM")) {
            ResultDto.requestType = "MM";

            filePath = System.getProperty("user.home") + "/Documents/Projects/JavaProjects/General/lottery/files/mm-sorted.txt";

        } else if (inputType.equalsIgnoreCase("PB")) {

            ResultDto.requestType = "PB";
            filePath = System.getProperty("user.home") + "/Documents/Projects/JavaProjects/General/lottery/files/pb-sorted.txt";
        } else if (inputType.equalsIgnoreCase("F5")) {
            ResultDto.requestType = "F5";
            //TODO if required
            filePath = "";
        }

//        if (args.length == 0) {
//            System.out.println("No arguments");
//            System.exit(1);
//        }

        System.out.println(" What action to do ? SIM, RAN, NUM_OCCUR, LAST ");
        String action = scanner.next();

        Path outputFile = createOutputFile(inputType, action);
        PrintStream consoleOut = System.out;
        PrintStream consoleErr = System.err;
        PrintStream capturedOutput = new PrintStream(
                new TeeOutputStream(consoleOut,
                        new PrettyTableOutputStream(new BufferedOutputStream(
                                Files.newOutputStream(outputFile)))), true);
        System.setOut(capturedOutput);
        System.setErr(capturedOutput);

        try {
            System.out.println("Lottery: " + inputType.toUpperCase(Locale.ROOT));
            System.out.println("Action: " + action.toUpperCase(Locale.ROOT));
            System.out.println("Input: " + filePath);
            System.out.println("Output: " + outputFile.toAbsolutePath());
            System.out.println("Started: " + LocalDateTime.now());
            System.out.println("##########################################################################");

        // Since occurrence  & total occurrence for all numbers after each individual lot
        // Since occurrence  & total occurrence for each group after each individual lot

        if ((action.equalsIgnoreCase("SIM"))) {

            LineNumberReader lnr = new LineNumberReader(new FileReader(new File(filePath)));
            lnr.skip(Long.MAX_VALUE);
            int totalDraws = lnr.getLineNumber();
            lnr.close();
            for (int i = 1; i <= totalDraws; i++) {
                ResultDto.setTotalDraw(i);
                ResultDto.setTotalDrawTemp(i);


                // Just count the number of occurrence for all numbers (except the red ball). Sorting by value. Its not printing anything but sets the ResultDto with value
                CountNumberOfOccurrenceOfWhiteBallsDelegate grpDelegate = new CountNumberOfOccurrenceOfWhiteBallsDelegate();
                grpDelegate.groupingDelegate(filePath);

                maxmindiffoccurence.MaxMinDiffOccurence mxmn = new maxmindiffoccurence.MaxMinDiffOccurence();
                mxmn.startwithit(filePath);

                System.out.println("T " + ResultDto.getResultGroupAll());
                System.out.println("S " + ResultDto.getSinceGroupAll());

                SinceOccurenceRangeTotal sOrT = new SinceOccurenceRangeTotal();
                sOrT.since_occurence_total(ResultDto.getSinceGroupAll());

                TotalOccurenceRangeTotal tort = new TotalOccurenceRangeTotal();
                tort.total_occurence_range_total(ResultDto.getResultGroupAll());
                System.out.println("##########################################################################");

            }

        }


        //Total Occurrence of all numbers across all draws sorted from minimum to maximum
        // Total Occurrence of all ranges for all draws
        else if ((action.equalsIgnoreCase("NUM_OCCUR"))) {

            LineNumberReader lnr = new LineNumberReader(new FileReader(new File(filePath)));
            lnr.skip(Long.MAX_VALUE);
            int totalDraws = lnr.getLineNumber();
            System.out.println(lnr.getLineNumber());    // Add 1 because line index starts at 0
            //     Finally, the LineNumberReader object should be closed to prevent resource leak
            lnr.close();

            //   for (int i=totalDraws/2;i<=totalDraws;i++)
            for (int i = 1; i <= totalDraws; i++) {
                ResultDto.setTotalDraw(i);
                ResultDto.setTotalDrawTemp(i);
                CountNumberOfOccurrenceOfWhiteBallsDelegate grpDelegate = new CountNumberOfOccurrenceOfWhiteBallsDelegate();
                grpDelegate.groupingDelegate(filePath);


                maxmindiffoccurence.MaxMinDiffOccurence mxmn = new maxmindiffoccurence.MaxMinDiffOccurence();
                mxmn.startwithit(filePath);


                SinceOccurenceRangeTotal sOrT = new SinceOccurenceRangeTotal();
                sOrT.since_occurence_total(ResultDto.getSinceGroupAll());

                TotalOccurenceRangeTotal tort = new TotalOccurenceRangeTotal();
                tort.total_occurence_range_total(ResultDto.getResultGroupAll());
                System.out.println("##########################################################################");


            }


        }

        //Grouping the range of numbers for all the draws since the start

        else if ((action.equalsIgnoreCase("RAN"))) {

            LineNumberReader lnr = new LineNumberReader(new FileReader(new File(filePath)));
            lnr.skip(Long.MAX_VALUE);
            int totalDraws = lnr.getLineNumber();
            System.out.println(lnr.getLineNumber());    // Add 1 because line index starts at 0
            //     Finally, the LineNumberReader object should be closed to prevent resource leak
            lnr.close();

            //   for (int i=totalDraws/2;i<=totalDraws;i++)
            for (int i = 1; i <= totalDraws; i++) {

                ResultDto.setTotalDraw(i);
                ResultDto.setTotalDrawTemp(i);


                groupingrange.GROUPINGRANGE grprng = new GROUPINGRANGE();
                grprng.groupingRangeStarts(filePath);
            }


        }


        //TODO - The block below is not being executed
        else if ((action.equalsIgnoreCase("LAST"))) {
            CountNumberOfOccurrenceOfWhiteBallsDelegate grpDelegate = new CountNumberOfOccurrenceOfWhiteBallsDelegate();
            grpDelegate.groupingDelegate(filePath);


            MaxMinDiffOccurence mxmn = new MaxMinDiffOccurence();
            mxmn.startwithit(filePath);
            System.out.println("**** Number ********** Total Occurrence  ************ Last occurrence since  ********************************************");

            SortByTot sbt = new SortByTot();
            sbt.sorByTot();
            printDataOutput(ResultDto.getSortbyTotalmap());

            System.out.println("**** Number ***** Total Occurrence  *********** Last occurrence since ****************************************************");

            SortBySince sbs = new SortBySince();
            sbs.sortBySince();
            printDataOutput(ResultDto.getSortbySincemap());
        }

            System.out.println("##########################################################################");
            System.out.println("Completed: " + LocalDateTime.now());
        } catch (ParseException | InterruptedException | IOException | RuntimeException exception) {
            System.err.println("Run failed: " + exception.getMessage());
            exception.printStackTrace(System.err);
            throw exception;
        } finally {
            capturedOutput.flush();
            System.setOut(consoleOut);
            System.setErr(consoleErr);
            capturedOutput.close();
        }

        consoleOut.println("Saved complete output to " + outputFile.toAbsolutePath());

    }

    private static Path createOutputFile(String lottery, String action) throws IOException {
        Path outputDirectory = Paths.get("files", "pb_stats");
        Files.createDirectories(outputDirectory);
        String fileName = lottery.toLowerCase(Locale.ROOT) + "-"
                + action.toLowerCase(Locale.ROOT) + ".txt";
        return outputDirectory.resolve(fileName);
    }


    private static void printDataOutput(LinkedHashMap<Integer, ArrayList> trio) {

        Iterator it = trio.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            ArrayList altemp = new ArrayList();
            altemp = (ArrayList) pair.getValue();
            Integer totalTemp = (Integer) altemp.get(0);
            Integer sinceTemp = (Integer) altemp.get(1);

            System.out.println(pair.getKey() + "   " + totalTemp + "    " + sinceTemp);
            it.remove();
        }

    }

    /** Writes to the console and output file without closing the console stream. */
    private static final class TeeOutputStream extends OutputStream {
        private final OutputStream console;
        private final OutputStream file;

        private TeeOutputStream(OutputStream console, OutputStream file) {
            this.console = console;
            this.file = file;
        }

        @Override
        public void write(int value) throws IOException {
            console.write(value);
            file.write(value);
        }

        @Override
        public void write(byte[] values, int offset, int length) throws IOException {
            console.write(values, offset, length);
            file.write(values, offset, length);
        }

        @Override
        public void flush() throws IOException {
            console.flush();
            file.flush();
        }

        @Override
        public void close() throws IOException {
            flush();
            file.close();
        }
    }

    /** Converts common analysis output into readable tables in the saved file. */
    private static final class PrettyTableOutputStream extends OutputStream {
        private final OutputStream output;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();
        private boolean numberSummaryTable;

        private PrettyTableOutputStream(OutputStream output) {
            this.output = output;
        }

        @Override
        public void write(int value) throws IOException {
            if (value == '\n') {
                writeFormattedLine(new String(line.toByteArray(), StandardCharsets.UTF_8));
                line.reset();
            } else if (value != '\r') {
                line.write(value);
            }
        }

        @Override
        public void write(byte[] values, int offset, int length) throws IOException {
            for (int index = offset; index < offset + length; index++) {
                write(values[index] & 0xff);
            }
        }

        private void writeFormattedLine(String value) throws IOException {
            if (writeMapTable(value, "T ", "TOTAL OCCURRENCES", "Number", "Occurrences")
                    || writeMapTable(value, "S ", "DRAWS SINCE LAST OCCURRENCE", "Number", "Draws since")
                    || writeMapTable(value, "TO ", "TOTAL-OCCURRENCE GROUPS", "Occurrence total", "Numbers")
                    || writeMapTable(value, "SO ", "SINCE-OCCURRENCE GROUPS", "Draws since", "Numbers")) {
                numberSummaryTable = false;
                return;
            }

            if (value.startsWith("**** Number")) {
                numberSummaryTable = true;
                writeLine("");
                writeLine("| Number | Total occurrences | Draws since last occurrence |");
                writeLine("|------:|------------------:|-----------------------------:|");
                return;
            }

            if (numberSummaryTable && value.trim().matches("\\d+\\s+\\d+\\s+\\d+")) {
                String[] fields = value.trim().split("\\s+");
                writeLine("| " + fields[0] + " | " + fields[1] + " | " + fields[2] + " |");
                return;
            }

            if (value.contains("\t")) {
                writeLine("| " + value.trim().replaceAll("\\s*\\t\\s*", " | ") + " |");
                return;
            }

            writeLine(value);
        }

        private boolean writeMapTable(String value, String prefix, String title,
                                      String firstHeader, String secondHeader) throws IOException {
            if (!value.startsWith(prefix + "{") || !value.endsWith("}")) {
                return false;
            }
            writeLine("");
            writeLine("## " + title);
            writeLine("");
            writeLine("| " + firstHeader + " | " + secondHeader + " |");
            writeLine("|---:|:---|");
            String content = value.substring(prefix.length() + 1, value.length() - 1);
            if (!content.trim().isEmpty()) {
                for (String entry : content.split(", ")) {
                    int separator = entry.indexOf('=');
                    if (separator >= 0) {
                        writeLine("| " + entry.substring(0, separator) + " | "
                                + entry.substring(separator + 1) + " |");
                    }
                }
            }
            return true;
        }

        private void writeLine(String value) throws IOException {
            output.write(value.getBytes(StandardCharsets.UTF_8));
            output.write('\n');
        }

        @Override
        public void flush() throws IOException {
            output.flush();
        }

        @Override
        public void close() throws IOException {
            if (line.size() > 0) {
                writeFormattedLine(new String(line.toByteArray(), StandardCharsets.UTF_8));
                line.reset();
            }
            output.close();
        }
    }


}
