
package sortfile;

import java.io.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author vipin
 * This class has functions to read a file
 */
public class FileReadAndSort {

    private Scanner scanner;

    void openFile(String filePath) {
        try {

            scanner = new Scanner(new File(filePath));
        } catch (FileNotFoundException ex) {
            System.out.println("File Not Found" + ex);


        }

    }


    public List<String> readFileAndSort() throws ParseException {

        scanner.useDelimiter("\\n");
        List<String> ls = new ArrayList<>();
        while (scanner.hasNext()) {
            String line = setLineDtoAndSort(scanner.next());
            ls.add(line);
        }
        scanner.close();
        return ls;


    }

    public List<String> reverseFile() throws ParseException {

        scanner.useDelimiter("\\n");
        List<String> ls = new ArrayList<>();
        while (scanner.hasNext()) {
            String line = scanner.next();
            ls.add(line);
        }
        scanner.close();
        return ls;


    }



    public String setLineDtoAndSort(String x) {

        Integer[] integers = new Integer[5];
        String[] singleLine = x.split("  ");
        integers[0] = Integer.parseInt(singleLine[1]);
        integers[1] = Integer.parseInt(singleLine[2]);
        integers[2] = Integer.parseInt(singleLine[3]);
        integers[3] = Integer.parseInt(singleLine[4]);
        integers[4] = Integer.parseInt(singleLine[5]);
        Arrays.sort(integers);

        String singleLineData = singleLine[0] + "  " + integers[0] + "  " + integers[1] + "  " + integers[2] + "  " + integers[3] + "  " + integers[4];

        return singleLineData;
    }


}
