/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sumapplication;


import java.text.ParseException;
import java.util.*;


/**
 * @author vipin
 * This module gives the total sum of balls at each lot and the total sum after all lots grouped and sorted by number of occurrence.
 */
public class SumApplication {
    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";

    HashMap<Integer, Integer> hm = new HashMap<>();
    public static int i = 0;

    public static void main(String[] args) throws ParseException {

        Scanner scannerInput = new Scanner(System.in);


        if (scannerInput.next().equalsIgnoreCase("MM")) {
            filePath = filePath + "mm-sorted.txt";
        } else if (scannerInput.next().equalsIgnoreCase("PB")) {
            filePath = filePath + "pb-sorted.txt";
        } else {
            System.out.println("invalid argument");
        }


        new SumApplication().sumStart(filePath);
    }


    public void sumStart(String fp) throws ParseException {
        FileRead fileRead = new FileRead();
        fileRead.openFile(fp);
        List<LineDTO> lineDTOs = fileRead.readFile();
        double TOTAL = getSum(lineDTOs);
        System.out.println("Total after  : " + "\t" + TOTAL);
    }

    public double getSum(List<LineDTO> lineDTOs) {
        int total = 0;
        int i = 0;
        int sum = 0;
        double av = 0;
        System.out.println("*************************************************************\tSum at each lot " + "\t*************************************************************");
        for (LineDTO ld : lineDTOs) {
            i++;
            sum = ld.getNumberone() + ld.getNumbertwo() + ld.getNumberthree()
                    + ld.getNumberfour() + ld.getNumberfive();
            total = total + sum;
            av = (total / i);

            System.out.println("Sum at the " + i + "th lot : " + sum + "  Average is " + av);
            arrangeSum(sum);
        }
        HashMap<Integer, Integer> hmap = sortByValues(hm);

        System.out.println("*************************************************************\t All sum arranged and sorted" + "\t*************************************************************");
        for (Integer key : hmap.keySet()) {
            System.out.println(key + "  : " + hmap.get(key));

        }

        return total;
    }

    public void arrangeSum(int sum) {
        int a = sum;
        if (!hm.containsKey(a)) {
            hm.put(a, 1);
        } else {
            Integer count = hm.get(a);
            count = count + 1;
            hm.put(a, count);

        }

    }

    private static HashMap sortByValues(HashMap map) {
        List list = new LinkedList(map.entrySet());
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





