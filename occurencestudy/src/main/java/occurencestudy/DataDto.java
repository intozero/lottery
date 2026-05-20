/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package occurencestudy;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
/**
 *
 * @author vipin
 */
public class DataDto {
    

    public int AssignDatetoList(List<String> tempo )            
    {
    //List<Date> datesofLot = new ArrayList<Date>();
        List<String> datesofLot = new ArrayList<String>();
     
    for ( int i =0; i<tempo.size();i=i+8)
    {
    datesofLot.add(tempo.get(i));
    System.out.println("Getting only the dates as String " + datesofLot);
    
    }
    
    
    return 000;
     
     
       
    }
    
    
}
