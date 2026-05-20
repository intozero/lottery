/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package asnumber;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author vipin
 */
public class AsNumber {
    private int n = 10;
    public HashMap<String, Integer> hmap = new HashMap<String, Integer>();

    /**
     * @param fp the command line arguments
     */
    public void numberCheck(String fp) throws ParseException {

        FileRead fr = new FileRead();
        fr.openFile(fp);
        List<LineDto> ldt = fr.readFile();
        numberasString(ldt);
    }

    public List<String> numberasString(List<LineDto> ldt) {
        List<String> Q = new ArrayList<String>();
        String singleString = "";
        for (LineDto ld : ldt) {
            String first = String.valueOf(ld.getNumberone());
            String second = String.valueOf(ld.getNumbertwo());
            String third = String.valueOf(ld.getNumberthree());
            String forth = String.valueOf(ld.getNumberfour());
            String fifth = String.valueOf(ld.getNumberfive());
            String sixth = String.valueOf(ld.getNumbersix());
            String numberasString = first + second + third + forth + fifth + sixth;
            //System.out.println("Just printing the number" + numberasString);
            singleString = singleString + numberasString;

            if (Q.contains(numberasString)) {
                System.out.println("Bingo Number repeated" + numberasString);

            } else {
                Q.add(numberasString);
            }

        }

        System.out.println("Bingo Number repeated" + singleString);
        stringmanipulator(singleString);
        return Q;
    }

    private void stringmanipulator(String singleString) {

        int q = 0;
        for (int i = 0; i < singleString.length() - n; i++) {
            String findStr = singleString.substring(i, i + n);
            int lastIndex = 0;
            int count = 0;
            // System.out.println ("Total lenght   "  + singleString.length());
            while (lastIndex != -1) {

                lastIndex = singleString.indexOf(findStr, lastIndex);

                //System.out.println ("Occurence   " + lastIndex );
                if (lastIndex != -1) {
                    count++;
                    lastIndex += findStr.length();
                }
            }
            if (!hmap.containsKey(findStr)) {
                hmap.put(findStr, count);
            } else {
                Integer c = hmap.get(findStr);
                c = c + count;
                hmap.put(findStr, count);

            }


        }

        System.out.println("Final   : " + hmap);

        HashMap<Integer, Integer> maptemp = sortByValues(hmap);
        try {
            System.out.println("I am inside run of Goup All Print Writer");
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("Substring.txt", true)));
            pw.println("******************ALL SUBSTRING " + n + "************************");
            Set setall = maptemp.entrySet();
            Iterator iteratorall = setall.iterator();
            while (iteratorall.hasNext()) {
                Map.Entry meall = (Map.Entry) iteratorall.next();
                pw.println(meall.getKey() + ": \t" + meall.getValue() + "\n");
            }
            pw.close();
        } catch (IOException ex) {

        }


    }

    private static HashMap sortByValues(HashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        // Here I am copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }

}
    
    

