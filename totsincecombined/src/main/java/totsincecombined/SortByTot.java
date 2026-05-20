/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package totsincecombined;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import static totsincecombined.AllNumbers.trio;


public class SortByTot {
    
  public void  sorByTot()
            {
               LinkedHashMap<Integer,ArrayList> sortbyTotalmap = new LinkedHashMap<Integer,ArrayList>();
             
//                  for (int i=1;i<=75;i++)
//                            
//                        {
//                            ArrayList tempinit = new ArrayList();
//                            tempinit.add(0, 0);
//                            tempinit.add(1, ResultDto.totaldraw);
//                            
//                          sortbyTotalmap.put(i, tempinit);
//                        
//                        }
               
               
               for (Integer i : ResultDto.getResultGroupAll().keySet())
                        {
                            Integer a = null; 
                                      Integer b = null;
                                      if (ResultDto.getResultGroupAll().get(i) != null)
                                          
                                      { a = ResultDto.getResultGroupAll().get(i) ; 
                                        } 
                                      else {
                                         a = 0;
                                      }
                                      
                                                                        
                                      if (ResultDto.getSinceGroupAll().get(i) != null)
                                          
                                      { b = ResultDto.getSinceGroupAll().get(i) ; 
                                        } 
                                      else {
                                         b = ResultDto.totaldraw;
                                      }
                            ArrayList temp = new ArrayList();
                            temp.add(0, a);
                            temp.add(1, b);
                           sortbyTotalmap.put(i, temp);
                             
                        }
              
              ResultDto.setSortbyTotalmap(sortbyTotalmap);
              

            }
  
  
    
    
}
