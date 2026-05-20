
package totsincecombined;

import groupingrange.GROUPINGRANGE;

import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.io.IOException;
import java.io.LineNumberReader;
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

            filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/mm-sorted.txt";

        } else if (inputType.equalsIgnoreCase("PB")) {

            ResultDto.requestType = "PB";
            filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/pb-sorted.txt";
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

            System.exit(1);

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


            System.exit(1);

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


            System.exit(1);

        }


        //TODO - The block below is not being executed
        else if ((action.equalsIgnoreCase("LAST"))) {
            CountNumberOfOccurrenceOfWhiteBallsDelegate grpDelegate = new CountNumberOfOccurrenceOfWhiteBallsDelegate();
            grpDelegate.groupingDelegate(filePath);


            MaxMinDiffOccurence mxmn = new MaxMinDiffOccurence();
            mxmn.startwithit(filePath);
            System.out.println("**** Number ********** Last occurrence since ************ Total Occurrence  ********************************************");

            SortByTot sbt = new SortByTot();
            sbt.sorByTot();
            printDataOutput(ResultDto.getSortbyTotalmap());

            System.out.println("**** Number ***** Total Occurrence  *********** Last occurrence since ****************************************************");

            SortBySince sbs = new SortBySince();
            sbs.sortBySince();
            printDataOutput(ResultDto.getSortbySincemap());
        }

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


}



