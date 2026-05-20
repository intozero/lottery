/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package grouping;

import totsincecombined.LineDto;
import totsincecombined.ResultDto;

import java.util.*;

/**
 * @author vipin
 */
public class CountNumberOfOccurrenceOfWhiteBalls implements Runnable {
    public static int e = 0;
    List<LineDto> listAllLineDto;


    public CountNumberOfOccurrenceOfWhiteBalls(List<LineDto> listAllLineDto) {
        this.listAllLineDto = listAllLineDto;
    }

    public void run() {

        LinkedHashMap<Integer, Integer> hmap = new LinkedHashMap<>();
        for (int i = 1; i <= 75; i++) {
            hmap.put(i, 0);
        }

        for (LineDto lineDto : listAllLineDto) {
            ++e;
            for (int i = 1; i < 6; i++) {
                int a = 0;

                switch (i) {

                    case 1:
                        a = lineDto.getNumberone();
                        break;
                    case 2:
                        a = lineDto.getNumbertwo();
                        break;
                    case 3:
                        a = lineDto.getNumberthree();
                        break;
                    case 4:
                        a = lineDto.getNumberfour();
                        break;

                    case 5:
                        a = lineDto.getNumberfive();
                        break;

                }

                if (!hmap.containsKey(a)) {
                    hmap.put(a, 1);
                } else {
                    Integer count = hmap.get(a);
                    count = count + 1;
                    hmap.put(a, count);

                }
            }


            LinkedHashMap<Integer, Integer> maptemp = sortByCount(hmap);

        }
        LinkedHashMap<Integer, Integer> map = sortByCount(hmap);
        ResultDto.setResultGroupAll(map);
    }

    private static LinkedHashMap sortByCount(LinkedHashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {

                    public int compare(Object o1, Object o2) {
                        return ((Comparable) ((Map.Entry) (o1)).getValue())
                                .compareTo(((Map.Entry) (o2)).getValue());
                    }
                }
        );

        // Here I am copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        LinkedHashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }


    //TODO Is it required as its not being used?

    private static HashMap sortByNumber(HashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getKey())
                        .compareTo(((Map.Entry) (o2)).getKey());
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
    

