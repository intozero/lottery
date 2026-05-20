/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package combinations;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author vipin
 * //
 */
public class TotalDevationSumCombinationCount {

    public static int counter = 0;
    public static HashMap<String, Integer> hmapsumdev = new HashMap<String, Integer>();
    public static HashMap<String, Integer> hmapsumdev_sorted = new HashMap<String, Integer>();
    public static String sum_dev;


    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException, FileNotFoundException, IOException {
        int[] v = new int[100];
        int n = 41;
        int maxK = 5;
        int w = 40;

        combinations(v, 1, n, 1, maxK);

        hmapsumdev_sorted = sortByValues(hmapsumdev);

        System.out.println("Hash Array    " + hmapsumdev_sorted);
        try {
            File f = new File(System.getProperty("user.home") + "/Documents/Projects/JavaProjects/General/lottery/Dev_Sum_Combination.txt");
            f.delete();
            PrintWriter pw1 = new PrintWriter(new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/Documents/Projects/JavaProjects/General/lottery/Dev_Sum_Combination.txt", true)));

            Integer zero = 0;
            for (String key : hmapsumdev_sorted.keySet()) {
                if (!key.equals(zero)) {
                    pw1.println(key + "|" + hmapsumdev_sorted.get(key));

                }

            }

            pw1.close();


        } catch (IOException ex) {

        }

    }


    private static void combinations(int v[], int start, int n, int k1, int maxK) throws InterruptedException {
//        int[] Q = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,
//                   26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,
//                   51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75};  
        int[] Q = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41};
        int i;
        int sum = 0;
        int deviation = 0;
        int mean = 0;

        if (k1 > maxK) {
            // System.out.println("Array  " + Arrays.toString(v));

            for (int j = 1; j <= maxK; j++) {

                sum = sum + Q[v[j] - 1];

            }

            mean = sum / 5;

            for (int j = 1; j <= maxK; j++) {
                deviation = deviation + (mean - Q[v[j] - 1]) * (mean - Q[v[j] - 1]);

            }

            deviation = (int) Math.sqrt(deviation / 5);

            String uniquedevsum = Integer.toString(deviation) + "_" + Integer.toString(sum);

            hashmapenter(uniquedevsum);


            return;
        }

        for (i = start; i <= n; i++) {
            v[k1] = i;
            combinations(v, i + 1, n, k1 + 1, maxK);
        }


    }

    private static void hashmapenter(String uniquedevsum) {
        if (hmapsumdev.containsKey(uniquedevsum)) {
            int q = hmapsumdev.get(uniquedevsum) + 1;
            hmapsumdev.put(uniquedevsum, q);
        } else {
            hmapsumdev.put(uniquedevsum, 1);

        }

    }

    private static HashMap<String, Integer> sortByValues(HashMap<String, Integer> hmap) {
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
    
