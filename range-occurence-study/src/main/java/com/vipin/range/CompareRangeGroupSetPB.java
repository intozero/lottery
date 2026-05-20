package com.vipin.range;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class CompareRangeGroupSetPB {

    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";

    public static void main(String[] args) throws FileNotFoundException {
        String filePathAllRange = filePath + "pb-all-range-combination.txt";
        String filePathCurrentRange = filePath + "pb-current-range-combination.txt";

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

//        listAllRange.forEach(s -> System.out.println(s));
        listAllRange.forEach(s -> {
            String[] eachS = s.split("  ");
            List<String> stringList = Arrays.asList(eachS);
            Integer value = 0;
            for (String q : stringList) {
                Integer k = Integer.valueOf(q.split(" ")[1]);
                value += k*k;
            }
            if( value == 7)
            {
                System.out.println(s);
            }

        });


    }
}
