/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package maxmindiffoccurence;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author vipin
 */
public class MaxMinDifferenceInOccurrenceAllNumbers {
    private int i;
    private Integer i1;
    private Integer i2;
    private Integer i3;
    private Integer i4;
    private Integer i5;
    private Integer i6;

    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";
    public static String directoryPath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";

    public static void main(String[] args) throws ParseException, IOException {

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

        new MaxMinDifferenceInOccurrenceAllNumbers().startExecution(filePath);
    }


    public void startExecution(String filePath) throws ParseException {

        FileRead fr = new FileRead();
        fr.openFile(filePath);

        List<LineDto> listAllLineDto = fr.readFile();
        List<OccurenceNumber> onList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            OccurenceNumber on = new OccurenceNumber();
            onList.add(i, on);

        }

        List<OccurenceNumber> onList6 = new ArrayList<OccurenceNumber>();
        for (int i = 0; i < 1000; i++) {
            OccurenceNumber on = new OccurenceNumber();
            onList6.add(i, on);

        }

        HashMap<Integer, OccurenceNumber> hmap = new HashMap<Integer, OccurenceNumber>();
        HashMap<Integer, OccurenceNumber> hmap6 = new HashMap<Integer, OccurenceNumber>();
        HashMap<Integer, Integer> hmap1 = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> hmap_6 = new HashMap<Integer, Integer>();
        int k = 0;
        for (LineDto listline : listAllLineDto) {
            for (int j = 1; j < 6; j++) {
                k++;


                switch (j) {
                    case 1:
                        i1 = new Integer(listline.getNumberone());
                        if (!hmap.containsKey(i1)) {
                            onList.get(i1).setLastOccureddt(k);
                            onList.get(i1).setOccurenceSinceLast(0);
                            onList.get(i1).setMaxDifferenceOccurence(k);
                            onList.get(i1).setMinDifferenceOccurence(k);
                            hmap.put(i1, onList.get(i1));
                            break;

                        } else {

                            onList.get(i1).setOccurenceSinceLast(k - onList.get(i1).getLastOccureddt());
                            onList.get(i1).setLastOccureddt(k);

                            if (onList.get(i1).getMinDifferenceOccurence() > onList.get(i1).getOccurenceSinceLast()) {
                                onList.get(i1).setMinDifferenceOccurence(onList.get(i1).getOccurenceSinceLast());
                            }


                            if (onList.get(i1).getOccurenceSinceLast() > onList.get(i1).getMaxDifferenceOccurence()) {
                                onList.get(i1).setMaxDifferenceOccurence(onList.get(i1).getOccurenceSinceLast());
                            }

                            onList.get(i1).setOccurenceSinceLast(0);
                            hmap.put(i1, onList.get(i1));

                            break;

                        }

                    case 2:
                        i2 = new Integer(listline.getNumbertwo());
                        if (!hmap.containsKey(i2)) {
                            onList.get(i2).setLastOccureddt(k);
                            onList.get(i2).setOccurenceSinceLast(0);
                            onList.get(i2).setMaxDifferenceOccurence(k);
                            onList.get(i2).setMinDifferenceOccurence(k);
                            hmap.put(i2, onList.get(i2));
                            break;

                        } else {

                            onList.get(i2).setOccurenceSinceLast(k - onList.get(i2).getLastOccureddt());
                            onList.get(i2).setLastOccureddt(k);

                            if (onList.get(i2).getMinDifferenceOccurence() > onList.get(i2).getOccurenceSinceLast()) {
                                onList.get(i2).setMinDifferenceOccurence(onList.get(i2).getOccurenceSinceLast());
                            }


                            if (onList.get(i2).getOccurenceSinceLast() > onList.get(i2).getMaxDifferenceOccurence()) {
                                onList.get(i2).setMaxDifferenceOccurence(onList.get(i2).getOccurenceSinceLast());
                            }

                            onList.get(i2).setOccurenceSinceLast(0);
                            hmap.put(i2, onList.get(i2));

                            break;

                        }
                    case 3:
                        i3 = new Integer(listline.getNumberthree());
                        if (!hmap.containsKey(i3)) {
                            onList.get(i3).setLastOccureddt(k);
                            onList.get(i3).setOccurenceSinceLast(0);
                            onList.get(i3).setMaxDifferenceOccurence(k);
                            onList.get(i3).setMinDifferenceOccurence(k);
                            hmap.put(i3, onList.get(i3));
                            break;

                        } else {

                            onList.get(i3).setOccurenceSinceLast(k - onList.get(i3).getLastOccureddt());
                            onList.get(i3).setLastOccureddt(k);

                            if (onList.get(i3).getMinDifferenceOccurence() > onList.get(i3).getOccurenceSinceLast()) {
                                onList.get(i3).setMinDifferenceOccurence(onList.get(i3).getOccurenceSinceLast());
                            }


                            if (onList.get(i3).getOccurenceSinceLast() > onList.get(i3).getMaxDifferenceOccurence()) {
                                onList.get(i3).setMaxDifferenceOccurence(onList.get(i3).getOccurenceSinceLast());
                            }

                            onList.get(i3).setOccurenceSinceLast(0);
                            hmap.put(i3, onList.get(i3));

                            break;

                        }
                    case 4:
                        i4 = new Integer(listline.getNumberfour());
                        if (!hmap.containsKey(i4)) {
                            onList.get(i4).setLastOccureddt(k);
                            onList.get(i4).setOccurenceSinceLast(0);
                            onList.get(i4).setMaxDifferenceOccurence(k);
                            onList.get(i4).setMinDifferenceOccurence(k);
                            hmap.put(i4, onList.get(i4));
                            break;

                        } else {

                            onList.get(i4).setOccurenceSinceLast(k - onList.get(i4).getLastOccureddt());
                            onList.get(i4).setLastOccureddt(k);

                            if (onList.get(i4).getMinDifferenceOccurence() > onList.get(i4).getOccurenceSinceLast()) {
                                onList.get(i4).setMinDifferenceOccurence(onList.get(i4).getOccurenceSinceLast());
                            }


                            if (onList.get(i4).getOccurenceSinceLast() > onList.get(i4).getMaxDifferenceOccurence()) {
                                onList.get(i4).setMaxDifferenceOccurence(onList.get(i4).getOccurenceSinceLast());
                            }

                            onList.get(i4).setOccurenceSinceLast(0);
                            hmap.put(i4, onList.get(i4));

                            break;

                        }

                    case 5:
                        i5 = new Integer(listline.getNumberfive());
                        if (!hmap.containsKey(i5)) {
                            onList.get(i5).setLastOccureddt(k);
                            onList.get(i5).setOccurenceSinceLast(0);
                            onList.get(i5).setMaxDifferenceOccurence(k);
                            onList.get(i5).setMinDifferenceOccurence(k);
                            hmap.put(i5, onList.get(i5));
                            break;

                        } else {

                            onList.get(i5).setOccurenceSinceLast(k - onList.get(i5).getLastOccureddt());
                            onList.get(i5).setLastOccureddt(k);

                            if (onList.get(i5).getMinDifferenceOccurence() > onList.get(i5).getOccurenceSinceLast()) {
                                onList.get(i5).setMinDifferenceOccurence(onList.get(i5).getOccurenceSinceLast());
                            }


                            if (onList.get(i5).getOccurenceSinceLast() > onList.get(i5).getMaxDifferenceOccurence()) {
                                onList.get(i5).setMaxDifferenceOccurence(onList.get(i5).getOccurenceSinceLast());
                            }

                            onList.get(i5).setOccurenceSinceLast(0);
                            hmap.put(i5, onList.get(i5));

                            break;

                        }

                }


                for (Integer key : hmap.keySet()) {
                    OccurenceNumber oc = hmap.get(key);
                    oc.setOccurenceSinceLast(oc.getOccurenceSinceLast() + 1);
                    hmap.put(key, oc);
                }
            }


        }

        int k1 = 0;
        for (LineDto listline : listAllLineDto) {
            k1++;
            i6 = new Integer(listline.getNumbersix());
            if (!hmap6.containsKey(i6)) {
                onList6.get(i6).setLastOccureddt(k1);
                onList6.get(i6).setOccurenceSinceLast(0);
                onList6.get(i6).setMaxDifferenceOccurence(k1);
                onList6.get(i6).setMinDifferenceOccurence(k1);
                hmap6.put(i6, onList6.get(i6));


            } else {

                onList6.get(i6).setOccurenceSinceLast(k1 - onList6.get(i6).getLastOccureddt());
                onList6.get(i6).setLastOccureddt(k1);

                if (onList6.get(i6).getMinDifferenceOccurence() > onList6.get(i6).getOccurenceSinceLast()) {
                    onList6.get(i6).setMinDifferenceOccurence(onList6.get(i6).getOccurenceSinceLast());
                }


                if (onList6.get(i6).getOccurenceSinceLast() > onList6.get(i6).getMaxDifferenceOccurence()) {
                    onList6.get(i6).setMaxDifferenceOccurence(onList6.get(i6).getOccurenceSinceLast());
                }

                onList6.get(i6).setOccurenceSinceLast(0);
                hmap6.put(i6, onList6.get(i6));

            }

            for (Integer key1 : hmap6.keySet()) {
                OccurenceNumber oc1 = hmap6.get(key1);
                oc1.setOccurenceSinceLast(oc1.getOccurenceSinceLast() + 1);
                hmap6.put(key1, oc1);
            }
        }


        for (Integer key : hmap.keySet()) {
            OccurenceNumber oc = hmap.get(key);


            if (oc.getMinDifferenceOccurence() - oc.maxDifferenceOccurence == 0) {
                System.out.println(key + " Minimum gap between two occurrence  " + ":::: Does not exist::::::");
            } else {
                System.out.println(key + " Minimum gap between two occurrence " + oc.getMinDifferenceOccurence() / 5);
            }

            System.out.println(key + " Maximum gap between two occurrence   " + oc.getMaxDifferenceOccurence() / 5);
            System.out.println(key + " Since last occurred   " + oc.getOccurenceSinceLast() / 5);
            System.out.println("****************************************************");
            hmap1.put(key, oc.getOccurenceSinceLast() / 5);

        }


        /** Append to File **/
//TODO  To Append to File. What it is? Can be done a different way?
/*
        ToAppendFile taf = new ToAppendFile();
        Integer zero = 0;
        File f = new File(directoryPath + "ALL_1.txt");
        f.delete();
        Map<Integer, Integer> hmap_toPrint = new TreeMap<Integer, Integer>(hmap1);

        for (Integer key : hmap_toPrint.keySet()) {
            if (!key.equals(zero)) {
                System.out.println(" Vipin     " + hmap1.get(key));
                taf.editfile_white(hmap_toPrint.get(key));

            }

        }
        File f1 = new File(filePath + "ALL_TO_SO_WHITE.txt");
        f1.delete();
        f.renameTo(f1);*/
        /** Append to File **/

        System.out.println("*********************** Sort the balls by number of lots since they last occurred ");
        hmap1 = sortByValues(hmap1);
        for (Integer key : hmap1.keySet()) {
            System.out.println(key + "    :  " + hmap1.get(key));

        }

        System.out.println("******************* Number Six Starts *********************************");
        for (Integer key : hmap6.keySet()) {
            OccurenceNumber oc = hmap6.get(key);


            if (oc.getMinDifferenceOccurence() - oc.maxDifferenceOccurence == 0) {
                System.out.println(key + "Minimum gap between two occurrence " + ":::: Doe not exist::::::");
            } else {
                System.out.println(key + "  Minimum  " + oc.getMinDifferenceOccurence());
            }

            System.out.println(key + " Maximum gap between two occurrence   " + oc.getMaxDifferenceOccurence());
            System.out.println(key + " Since Last occurred  " + oc.getOccurenceSinceLast());
            System.out.println("****************************************************");
            hmap_6.put(key, oc.getOccurenceSinceLast());
        }
        /** Append to File **/

//TODO  To Append to File. What it is? Can be done a different way?
/*        ToAppendFile taf6 = new ToAppendFile();
        Integer zero6 = 0;
        File f6 = new File(filePath + "ALL_6.txt");
        f6.delete();
        Map<Integer, Integer> hmap_toPrint6 = new TreeMap<Integer, Integer>(hmap_6);

        for (Integer key : hmap_toPrint6.keySet()) {
            if (!key.equals(zero6) && !filePath.equalsIgnoreCase(filePath + "Fantasy5_Sorted.txt")) {
                System.out.println(" Vipin     " + hmap_6.get(key));
                taf6.editfile_red(hmap_toPrint6.get(key));

            }

        }
        File f16 = new File(filePath + "ALL_TO_SO_RED.txt");
        f16.delete();
        f6.renameTo(f16);*/
        /** Append to File **/


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
    
   

