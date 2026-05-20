/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package occurencestudy;

import java.text.ParseException;
import java.util.*;

/**
 * @author vipin
 */
public class OccurenceStudy {
    private int k;
    public static ArrayList<Integer> wholeList = new ArrayList<Integer>();

    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";


    public static void main(String[] args) throws ParseException {

        System.out.println("Enter either MM or PB");
        Scanner scannerInput = new Scanner(System.in);
        String input = scannerInput.next();

        if (input.equalsIgnoreCase("MM")) {
            filePath = filePath + "mm-sorted.txt";
        } else if (input.equalsIgnoreCase("PB")) {
            filePath = filePath + "pb-sorted.txt";
        } else {

            System.out.println("invalid argument");
        }

        new OccurenceStudy().startOccurrences(filePath);
    }


    public void startOccurrences(String fp) throws ParseException {

        FileRead fr = new FileRead();
        fr.openFile(fp);
        List<LineDto> listlinedto = fr.readFile();

        HashMap<Integer, List<Integer>> hMapOccurrencePoint = new HashMap<Integer, List<Integer>>();
        HashMap<Integer, List<Integer>> hMapLotOccurrencePoint = new HashMap<Integer, List<Integer>>();
        HashMap<Integer, Integer> hMapGapInOccurrence = new HashMap<Integer, Integer>();


        /**
         Below for loop will make a hash map which will have the number as key and the list of occurrence points as value
         */

        for (LineDto listline : listlinedto) {
            for (int j = 1; j < 6; j++) {
                k++;
                switch (j) {
                    case 1:
                        if (!hMapOccurrencePoint.containsKey(listline.getNumberone())) {
                            List<Integer> listOccurrence = new ArrayList<>();
                            listOccurrence.add(k);
                            hMapOccurrencePoint.put(listline.getNumberone(), listOccurrence);
                        } else {
                            hMapOccurrencePoint.get(listline.getNumberone()).add(k);

                        }
                        break;
                    case 2:
                        if (!hMapOccurrencePoint.containsKey(listline.getNumbertwo())) {
                            List<Integer> listOccurrence = new ArrayList<>();
                            listOccurrence.add(k);
                            hMapOccurrencePoint.put(listline.getNumbertwo(), listOccurrence);
                        } else {

                            hMapOccurrencePoint.get(listline.getNumbertwo()).add(k);
                        }
                        break;
                    case 3:
                        if (!hMapOccurrencePoint.containsKey(listline.getNumberthree())) {
                            List<Integer> listOccurrence = new ArrayList<>();
                            listOccurrence.add(k);
                            hMapOccurrencePoint.put(listline.getNumberthree(), listOccurrence);
                        } else {
                            hMapOccurrencePoint.get(listline.getNumberthree()).add(k);

                        }
                        break;

                    case 4:
                        if (!hMapOccurrencePoint.containsKey(listline.getNumberfour())) {
                            List<Integer> listOccurrence = new ArrayList<>();
                            listOccurrence.add(k);
                            hMapOccurrencePoint.put(listline.getNumberfour(), listOccurrence);
                        } else {

                            hMapOccurrencePoint.get(listline.getNumberfour()).add(k);
                        }
                        break;
                    case 5:
                        if (!hMapOccurrencePoint.containsKey(listline.getNumberfive())) {
                            List<Integer> listOccurrence = new ArrayList<>();
                            listOccurrence.add(k);
                            hMapOccurrencePoint.put(listline.getNumberfive(), listOccurrence);
                        } else {

                            hMapOccurrencePoint.get(listline.getNumberfive()).add(k);
                        }
                        break;
                }
            }
        }
        /**
          it will create a whole list of all occurrence points to get the maximum value. So that the last entry will have the since occurrence value
         */

        for (Integer key : hMapOccurrencePoint.keySet()) {
            getEntireList(hMapOccurrencePoint.get(key), wholeList);

        }

        Integer maxOccurrenceValue = Collections.max(wholeList);

        /**
         Below for loop will make a hash map which will have the number as key and the list of actual lot instance as value
         */

        for (Integer key : hMapOccurrencePoint.keySet()) {
            List<Integer> ilmod = new ArrayList<>();
            int a = 0;
            for (Integer i : hMapOccurrencePoint.get(key)) {

                int c = (i - a) / 5;

                ilmod.add(c);
                a = i;

            }
            int d = (maxOccurrenceValue- a) / 5;
            ilmod.add(d);
            hMapLotOccurrencePoint.put(key, ilmod);


        }


        for (Integer key : hMapLotOccurrencePoint.keySet()) {
            System.out.println(key + "      " + hMapLotOccurrencePoint.get(key));
        }


        for (Integer key : hMapLotOccurrencePoint.keySet()) {

            for (Integer key1 : hMapLotOccurrencePoint.get(key)) {

                if (!hMapGapInOccurrence.containsKey(key1)) {
                    hMapGapInOccurrence.put(key1, 1);

                } else {
                    hMapGapInOccurrence.put(key1, hMapGapInOccurrence.get(key1) + 1);
                }


            }


        }

        hMapGapInOccurrence = sortByValues(hMapGapInOccurrence);
        System.out.println("******************************************** RANGE SUM  ****************************************************************************************************");


        int totalGapsComputed = 0;
        for (Integer key : hMapGapInOccurrence.keySet()) {
            totalGapsComputed = totalGapsComputed + hMapGapInOccurrence.get(key);

        }

        System.out.println(totalGapsComputed + " ******");

        for (Integer key : hMapGapInOccurrence.keySet()) {

            double aDouble = (double)hMapGapInOccurrence.get(key)/totalGapsComputed;
            System.out.println(key + "\t" + hMapGapInOccurrence.get(key) + "\t" + aDouble*100);

        }


    }

    private void getEntireList(List<Integer> get, ArrayList<Integer> wholeList) {

        for (Integer i : get) {
            wholeList.add(i);
        }

    }

    private HashMap<Integer, Integer> sortByValues(HashMap<Integer, Integer> hmap) {
        List list = new LinkedList(hmap.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;


    }

}
