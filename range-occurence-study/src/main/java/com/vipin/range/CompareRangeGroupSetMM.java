package com.vipin.range;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CompareRangeGroupSetMM {

    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";

    public static void main(String[] args) throws FileNotFoundException {
        String filePathAllRange = filePath + "mm-all-range-combination.txt";
        String filePathCurrentRange = filePath + "mm-current-range-combination.txt";

        Scanner scanner = new Scanner(new File(filePathAllRange));

        scanner.useDelimiter("\\n");
        List<String> listAllRange = new ArrayList<>();
        while (scanner.hasNext()) {
            String line = scanner.next().substring(0, 48);
            listAllRange.add(line);
        }
        scanner.close();

        Scanner scanner1 = new Scanner(new File(filePathCurrentRange));

        scanner1.useDelimiter("\\n");
        List<String> listCurrentRange = new ArrayList<>();
        while (scanner1.hasNext()) {
            String line = scanner1.next().substring(0, 48);
            listCurrentRange.add(line);
        }


        scanner1.close();

        listAllRange.removeAll(listCurrentRange);

        listAllRange.forEach( s -> System.out.println(s));



    }
}
