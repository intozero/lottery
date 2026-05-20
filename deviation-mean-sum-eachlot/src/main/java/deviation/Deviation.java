
package deviation;

import java.text.ParseException;
import java.util.*;
import java.lang.Math;


/**
 * @author vipin
 */
public class Deviation {

    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";


    HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
    HashMap<Integer, Integer> hm1 = new HashMap<Integer, Integer>();
    public static int i = 0;
    public static HashMap<String, Integer> hmapsumdev = new HashMap<String, Integer>();
    public static HashMap<String, Integer> hmapsumdev_sorted = new HashMap<String, Integer>();


    public static void main(String[] args) throws ParseException {

        System.out.println("Enter either MM or PB");
        Scanner scannerInput = new Scanner(System.in);

        if (scannerInput.next().equalsIgnoreCase("MM")) {
            filePath = filePath + "mm-sorted.txt";
        } else if (scannerInput.next().equalsIgnoreCase("PB")) {
            filePath = filePath + "pb-sorted.txt";
        } else {

            System.out.println("invalid argument");
        }

        new Deviation().deviationStart(filePath);
    }


    public void deviationStart(String fb) throws ParseException {
        FileRead fr = new FileRead();
        fr.openFile(fb);
        List<LineDTO> ldto = fr.readFile();
        double deviation_var = getDeviation(ldto);
        hmapsumdev_sorted = sortByValues(hmapsumdev);
        for (String key : hmapsumdev_sorted.keySet()) {

            System.out.println(key + " :  " + hmapsumdev_sorted.get(key));

        }
    }

    public double getDeviation(List<LineDTO> ldto) {
        int total = 0;
        int i = 0;
        int sumnow = 0;
        double av = 0;
        int deviation_instance = 0;
        for (LineDTO ld : ldto) {
            i++;
            sumnow = ld.getNumberone() + ld.getNumbertwo() + ld.getNumberthree()
                    + ld.getNumberfour() + ld.getNumberfive();
            total = total + sumnow;
            av = (total / i);
            int mean_instance = sumnow / 5;
            deviation_instance = (int) Math.sqrt((Math.pow(mean_instance - ld.getNumberone(), 2)
                    + Math.pow(mean_instance - ld.getNumbertwo(), 2)
                    + Math.pow(mean_instance - ld.getNumberthree(), 2)
                    + Math.pow(mean_instance - ld.getNumberfour(), 2)
                    + Math.pow(mean_instance - ld.getNumberfive(), 2)) / 5);
            System.out.println("Deviation , Mean  and Sum   at  " + i + " th lot" + "\t  \t " + deviation_instance + "   " + mean_instance + "   " + sumnow);
            arrangeDeviationNow((int) deviation_instance);
            arrangeMeanNow(mean_instance);
            String uniquedevsum = Integer.toString(deviation_instance) + "_" + Integer.toString(sumnow);

            hashmapenter(uniquedevsum);
        }
        HashMap<Integer, Integer> hmap = sortByValues(hm);
        System.out.println("\t******************** \t******************** Deviation Arranged by count " + "\t********************");

        for (Integer key : hmap.keySet()) {
            System.out.println(key + "  :  " + hmap.get(key));

        }


        return total;
    }

    public void arrangeDeviationNow(int sumnow) {
        int a = sumnow;
        if (!hm.containsKey(a)) {
            hm.put(a, 1);
        } else {
            Integer count = hm.get(a);
            count = count + 1;
            hm.put(a, count);

        }

    }

    public void arrangeMeanNow(int meannow) {
        int a = meannow;
        if (!hm1.containsKey(a)) {
            hm1.put(a, 1);
        } else {
            Integer count = hm1.get(a);
            count = count + 1;
            hm1.put(a, count);

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

    private void hashmapenter(String uniquedevsum) {
        if (hmapsumdev.containsKey(uniquedevsum)) {
            int q = hmapsumdev.get(uniquedevsum) + 1;
            hmapsumdev.put(uniquedevsum, q);
        } else {
            hmapsumdev.put(uniquedevsum, 1);

        }
    }

}





