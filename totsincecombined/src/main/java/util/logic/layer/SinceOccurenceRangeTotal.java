/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.logic.layer;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import totsincecombined.ResultDto;

/**
 *
 * @author vipin
 */
public class SinceOccurenceRangeTotal {
    
      Integer sum = 0 ;
      Integer sum_1_09 =0;
      Integer sum_10_19 =0;
      Integer sum_20_29 =0;
      Integer sum_30_39 =0;
      Integer sum_40_49 =0;
      Integer sum_50_59 =0;
      Integer sum_60_69 =0;
      Integer sum_70_79 =0;
    private int sum1;
     
    public void since_occurence_total(LinkedHashMap<Integer,Integer> hmapall)
    {
      for (Integer key : hmapall.keySet())
      {  
        //System.out.println("value of key and set " + key + "    " + hmapall.get(key));  
        sum = sum +   hmapall.get(key);
        
           if (key <= 9)
           {
              sum_1_09 = sum_1_09 + hmapall.get(key);
           }
           else if (key <= 19 && key >= 10)
           {
              sum_10_19 = sum_10_19 + hmapall.get(key);
           }
            else if (key <= 29 && key >= 20)
           {
              sum_20_29 = sum_20_29 + hmapall.get(key);
           }
            else if (key <= 39 && key >= 30)
           {
              sum_30_39 = sum_30_39 + hmapall.get(key);
           }
            else if (key <= 49 && key >= 40)
           {
              sum_40_49 = sum_40_49 + hmapall.get(key);
           }
            else if (key <= 59 && key >= 50)
           {
              sum_50_59 = sum_50_59 + hmapall.get(key);
           }
            else if (key <= 69 && key >= 60)
           {
              sum_60_69 = sum_60_69 + hmapall.get(key);
           }
            else if (key <= 79 && key >= 70)
           {
              sum_70_79 = sum_70_79 + hmapall.get(key);
              
             // System.out.println("value of 79 " + key + "    " + hmapall.get(key));
           }
        
        
      
      }
        
   // System.out.println("SUM OF SINCE    ::::" + sum);
    
    
    LinkedHashMap<Integer, Integer> maptemp = new LinkedHashMap<Integer, Integer>();
    maptemp.put(9,sum_1_09);
    maptemp.put(19,sum_10_19);
    maptemp.put(29,sum_20_29);
    maptemp.put(39,sum_30_39);
    maptemp.put(49,sum_40_49);
    maptemp.put(59,sum_50_59);
    maptemp.put(69,sum_60_69);
    
    if(ResultDto.requestType=="MM")
    {
        
        maptemp.put(79,sum_70_79);
    }
    
    maptemp = sortByValuesDescending(maptemp);
    System.out.println("SO " + maptemp);
    
    ResultDto.setsinceOccurenceRangeTotal(maptemp);
    
    }
    
        private LinkedHashMap<Integer, Integer> sortByValuesDescending(LinkedHashMap<Integer, Integer> hmap) {
            List list = new LinkedList(hmap.entrySet());
       Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
               return ((Comparable) ((Map.Entry) (o2)).getValue())
                  .compareTo(((Map.Entry) (o1)).getValue());
            }
       });
       
       LinkedHashMap sortedHashMap = new LinkedHashMap();
       for (Iterator it = list.iterator(); it.hasNext();) {
              Map.Entry entry = (Map.Entry) it.next();
              sortedHashMap.put(entry.getKey(), entry.getValue());
       } 
       return sortedHashMap;
  
             
    }
    
    
    
}
