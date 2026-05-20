
package maxmindiffoccurence;
import java.io.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author vipin
 */
public class FileRead {
    
private Scanner s;
List<String> temps = new ArrayList<String>();
    SimpleDateFormat sdformat = new SimpleDateFormat("MM/dd/yyyy");
       
int i =0;

 
 void openFile(String fp) {
    try {
        s = new Scanner(new File (fp));
   
    } catch (FileNotFoundException ex) {
        System.out.println("File Not Found" + ex);
        
             
    } 
   
}

 
 public List<LineDto> readFile () throws ParseException{
     
     //s.useDelimiter("  |\\n");
     s.useDelimiter("\\n");
 List<LineDto> listlinedto = new ArrayList<LineDto>() ;
 while(s.hasNext())
     
 {
  String a = s.next();

 LineDto linedto = setLineDto(a);

     listlinedto.add(i,linedto);

    i=i+1 ; 

 }
 s.close();
 return listlinedto;
 
 }

 void printArray()
         
 {
  System.out.println("Inside printArray");
  String[] tempsArray = temps.toArray(new String[0]);
int j =0;
    for (String s : tempsArray) {
      j++;
        System.out.println(s);
      
      System.out.println();
    }
// System.out.println("total number of elements in the array"+j);
 DataDto datedto = new DataDto();
   int c= datedto.AssignDatetoList(temps);
    //System.out.println("Return code from DataDto " + c);
 }
 
 public LineDto setLineDto(String x) throws ParseException{
 
    
    
         String[] temp = x.split("  ");     
             
             
            // System.out.println("Is x correct? \n"+ x);
            java.util.Date dt = sdformat.parse(temp[0]);
             
          LineDto ldto = new LineDto();
    
       ldto.setLotDate(dt);
        //System.out.println("Debugging the linedto" + Integer.valueOf(temp[1]) );
        //System.out.println("Debugging the linedto \n" + temp[1] + "\n temp' values");
        ldto.setNumberone(Integer.valueOf(temp[1]));
        ldto.setNumbertwo(Integer.valueOf(temp[2]));
        ldto.setNumberthree(Integer.valueOf(temp[3])); 
        ldto.setNumberfour(Integer.valueOf(temp[4]));
        //ldto.setNumberfive(Integer.valueOf(temp[5]));
       if (temp.length==5)
        {ldto.setNumberfive(99);
        ldto.setNumbersix(99);
        return ldto;
        }
        else ldto.setNumberfive(Integer.valueOf(temp[5]));
        
        if (temp.length==6)
        {ldto.setNumbersix(99);
        }
        else ldto.setNumbersix(Integer.valueOf(temp[6]));
        
          //System.out.println("String Date from Dto" + ldto.getLotDate());
          
          //System.out.println(ldto);
          return ldto;
            
 }
 
 
 
}
