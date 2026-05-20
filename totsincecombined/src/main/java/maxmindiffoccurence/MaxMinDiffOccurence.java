/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package maxmindiffoccurence;

import java.io.File;
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
import java.util.Map.Entry;
import java.util.TreeMap;

import totsincecombined.FileRead;
import totsincecombined.LineDto;
import totsincecombined.ResultDto;


/**
 * @author vipin
 */
public class MaxMinDiffOccurence {
    private int i;
    private Integer i1;
    private Integer i2;
    private Integer i3;
    private Integer i4;
    private Integer i5;
    private Integer i6;


    public void startwithit(String fp) throws ParseException {

        FileRead fr = new FileRead();
        fr.openFile(fp);
        List<LineDto> listlinedto = fr.readFile();
        List<OccurenceNumber> onList = new ArrayList<OccurenceNumber>();
        for (int i = 0; i < 10000; i++) {
            OccurenceNumber on = new OccurenceNumber();
            onList.add(i, on);

        }

        List<OccurenceNumber> onList6 = new ArrayList<OccurenceNumber>();
        for (int i = 0; i < 10000; i++) {
            OccurenceNumber on = new OccurenceNumber();
            onList6.add(i, on);

        }

        HashMap<Integer, OccurenceNumber> hmap = new HashMap<Integer, OccurenceNumber>();


        HashMap<Integer, OccurenceNumber> hmap6 = new HashMap<Integer, OccurenceNumber>();
        LinkedHashMap<Integer, Integer> hmap1 = new LinkedHashMap<Integer, Integer>();
        HashMap<Integer, Integer> hmap_6 = new HashMap<Integer, Integer>();
        int k = 0;
        for (LineDto listline : listlinedto) {
            for (int j = 1; j < 6; j++) {
                k++;


                switch (j) {
                    case 1:
                        i1 = new Integer(listline.getNumberone());
                        if (!hmap.containsKey(i1)) {
                            onList.get(i1).setLastOccureddt(k);
                            onList.get(i1).setOccurenceSinceLasdt(0);
                            onList.get(i1).setMaxDifferenceOccurence(k);
                            onList.get(i1).setMinDifferenceOccurence(k);
                            hmap.put(i1, onList.get(i1));
                            break;

                        } else {

                            onList.get(i1).setOccurenceSinceLasdt(k - onList.get(i1).getLastOccureddt());
                            onList.get(i1).setLastOccureddt(k);

                            if (onList.get(i1).getMinDifferenceOccurence() > onList.get(i1).getOccurenceSinceLast()) {
                                onList.get(i1).setMinDifferenceOccurence(onList.get(i1).getOccurenceSinceLast());
                            }


                            if (onList.get(i1).getOccurenceSinceLast() > onList.get(i1).getMaxDifferenceOccurence()) {
                                onList.get(i1).setMaxDifferenceOccurence(onList.get(i1).getOccurenceSinceLast());
                            }

                            onList.get(i1).setOccurenceSinceLasdt(0);
                            hmap.put(i1, onList.get(i1));

                            break;

                        }

                    case 2:
                        i2 = new Integer(listline.getNumbertwo());
                        if (!hmap.containsKey(i2)) {
                            onList.get(i2).setLastOccureddt(k);
                            onList.get(i2).setOccurenceSinceLasdt(0);
                            onList.get(i2).setMaxDifferenceOccurence(k);
                            onList.get(i2).setMinDifferenceOccurence(k);
                            hmap.put(i2, onList.get(i2));
                            break;

                        } else {

                            onList.get(i2).setOccurenceSinceLasdt(k - onList.get(i2).getLastOccureddt());
                            onList.get(i2).setLastOccureddt(k);

                            if (onList.get(i2).getMinDifferenceOccurence() > onList.get(i2).getOccurenceSinceLast()) {
                                onList.get(i2).setMinDifferenceOccurence(onList.get(i2).getOccurenceSinceLast());
                            }


                            if (onList.get(i2).getOccurenceSinceLast() > onList.get(i2).getMaxDifferenceOccurence()) {
                                onList.get(i2).setMaxDifferenceOccurence(onList.get(i2).getOccurenceSinceLast());
                            }

                            onList.get(i2).setOccurenceSinceLasdt(0);
                            hmap.put(i2, onList.get(i2));

                            break;

                        }
                    case 3:
                        i3 = new Integer(listline.getNumberthree());
                        if (!hmap.containsKey(i3)) {
                            onList.get(i3).setLastOccureddt(k);
                            onList.get(i3).setOccurenceSinceLasdt(0);
                            onList.get(i3).setMaxDifferenceOccurence(k);
                            onList.get(i3).setMinDifferenceOccurence(k);
                            hmap.put(i3, onList.get(i3));
                            break;

                        } else {

                            onList.get(i3).setOccurenceSinceLasdt(k - onList.get(i3).getLastOccureddt());
                            onList.get(i3).setLastOccureddt(k);

                            if (onList.get(i3).getMinDifferenceOccurence() > onList.get(i3).getOccurenceSinceLast()) {
                                onList.get(i3).setMinDifferenceOccurence(onList.get(i3).getOccurenceSinceLast());
                            }


                            if (onList.get(i3).getOccurenceSinceLast() > onList.get(i3).getMaxDifferenceOccurence()) {
                                onList.get(i3).setMaxDifferenceOccurence(onList.get(i3).getOccurenceSinceLast());
                            }

                            onList.get(i3).setOccurenceSinceLasdt(0);
                            hmap.put(i3, onList.get(i3));

                            break;

                        }
                    case 4:
                        i4 = new Integer(listline.getNumberfour());
                        if (!hmap.containsKey(i4)) {
                            onList.get(i4).setLastOccureddt(k);
                            onList.get(i4).setOccurenceSinceLasdt(0);
                            onList.get(i4).setMaxDifferenceOccurence(k);
                            onList.get(i4).setMinDifferenceOccurence(k);
                            hmap.put(i4, onList.get(i4));
                            break;

                        } else {

                            onList.get(i4).setOccurenceSinceLasdt(k - onList.get(i4).getLastOccureddt());
                            onList.get(i4).setLastOccureddt(k);

                            if (onList.get(i4).getMinDifferenceOccurence() > onList.get(i4).getOccurenceSinceLast()) {
                                onList.get(i4).setMinDifferenceOccurence(onList.get(i4).getOccurenceSinceLast());
                            }


                            if (onList.get(i4).getOccurenceSinceLast() > onList.get(i4).getMaxDifferenceOccurence()) {
                                onList.get(i4).setMaxDifferenceOccurence(onList.get(i4).getOccurenceSinceLast());
                            }

                            onList.get(i4).setOccurenceSinceLasdt(0);
                            hmap.put(i4, onList.get(i4));

                            break;

                        }

                    case 5:
                        i5 = new Integer(listline.getNumberfive());
                        if (!hmap.containsKey(i5)) {
                            onList.get(i5).setLastOccureddt(k);
                            onList.get(i5).setOccurenceSinceLasdt(0);
                            onList.get(i5).setMaxDifferenceOccurence(k);
                            onList.get(i5).setMinDifferenceOccurence(k);
                            hmap.put(i5, onList.get(i5));
                            break;

                        } else {

                            onList.get(i5).setOccurenceSinceLasdt(k - onList.get(i5).getLastOccureddt());
                            onList.get(i5).setLastOccureddt(k);

                            if (onList.get(i5).getMinDifferenceOccurence() > onList.get(i5).getOccurenceSinceLast()) {
                                onList.get(i5).setMinDifferenceOccurence(onList.get(i5).getOccurenceSinceLast());
                            }


                            if (onList.get(i5).getOccurenceSinceLast() > onList.get(i5).getMaxDifferenceOccurence()) {
                                onList.get(i5).setMaxDifferenceOccurence(onList.get(i5).getOccurenceSinceLast());
                            }

                            onList.get(i5).setOccurenceSinceLasdt(0);
                            hmap.put(i5, onList.get(i5));

                            break;

                        }

                }


                for (Integer key : hmap.keySet()) {
                    OccurenceNumber oc = hmap.get(key);
                    oc.setOccurenceSinceLasdt(oc.getOccurenceSinceLast() + 1);
                    hmap.put(key, oc);
                }
            }


        }

        int k1 = 0;
        // OccurenceNumber ocn6 = new OccurenceNumber();
        for (LineDto listline : listlinedto) {
            k1++;
            i6 = new Integer(listline.getNumbersix());
            if (!hmap6.containsKey(i6)) {
                onList6.get(i6).setLastOccureddt(k1);
                onList6.get(i6).setOccurenceSinceLasdt(0);
                onList6.get(i6).setMaxDifferenceOccurence(k1);
                onList6.get(i6).setMinDifferenceOccurence(k1);
                hmap6.put(i6, onList6.get(i6));


            } else {

                onList6.get(i6).setOccurenceSinceLasdt(k1 - onList6.get(i6).getLastOccureddt());
                onList6.get(i6).setLastOccureddt(k1);

                if (onList6.get(i6).getMinDifferenceOccurence() > onList6.get(i6).getOccurenceSinceLast()) {
                    onList6.get(i6).setMinDifferenceOccurence(onList6.get(i6).getOccurenceSinceLast());
                }


                if (onList6.get(i6).getOccurenceSinceLast() > onList6.get(i6).getMaxDifferenceOccurence()) {
                    onList6.get(i6).setMaxDifferenceOccurence(onList6.get(i6).getOccurenceSinceLast());
                }

                onList6.get(i6).setOccurenceSinceLasdt(0);
                hmap6.put(i6, onList6.get(i6));

            }

            for (Integer key1 : hmap6.keySet()) {
                OccurenceNumber oc1 = hmap6.get(key1);
                oc1.setOccurenceSinceLasdt(oc1.getOccurenceSinceLast() + 1);
                hmap6.put(key1, oc1);
            }
        }


        for (int i = 1; i <= 75; i++) {

            hmap1.put(i, ResultDto.totaldrawtemp);

        }


        for (Integer key : hmap.keySet()) {
            OccurenceNumber oc = hmap.get(key);
            hmap1.put(key, oc.getOccurenceSinceLast() / 5);

        }


        hmap1 = sortByValues(hmap1);


        ResultDto.setResultSinceAll(hmap1);
    }

    private LinkedHashMap<Integer, Integer> sortByValues(LinkedHashMap<Integer, Integer> hmap) {
        List list = new LinkedList(hmap.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue())
                        .compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        LinkedHashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;


    }

    private LinkedHashMap<Integer, Integer> sortByValuesDescending(LinkedHashMap<Integer, Integer> hmap) {
        return null;

    }


}
    
   

