package maxmindiffoccurence;

import java.io.*;
import java.util.Set;

public class ToAppendFile 
{

    public static String filePath = System.getProperty("user.home") + "/Documents/Projects/JavaProjects/General/lottery/";
  static int n =0;
  static int n1 =0;
    void editfile_white(Integer keySet) {
        
    

         

         try {
              
              FileReader fr = new FileReader(filePath+"ALL_TO_SO_WHITE.txt");
              BufferedReader br = new BufferedReader(fr);
                        String s = "|";
              String s2 = keySet.toString();
              
              System.out.println("keySet.toString()      "+ keySet.toString());
              
              
              for(int i = 0; i < n; ++i)
                   br.readLine();
              
              String str = br.readLine();
              StringBuffer sb = new StringBuffer(str);
              sb.append(s2+s);
              String y = sb.toString();
              System.out.println(sb);
              System.out.println("Appending");
              try {
                  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath+"ALL_1.txt", true)));
                  try {
                      out.println(y);
                  } finally {
                      out.close();
                  }
              } catch (IOException e) {
                                               } 
              
              
              System.out.println("Done");
              n++;

         }catch(IOException e) {}

    
    }
    void editfile_red(Integer keySet) {
        
    

         

         try {
              
              FileReader fr = new FileReader(filePath+"ALL_TO_SO_RED.txt");
              BufferedReader br = new BufferedReader(fr);
                        String s = "|";
              String s2 = keySet.toString();
              
              System.out.println("keySet.toString()      "+ keySet.toString());
              
              
              for(int i = 0; i < n1; ++i)
                   br.readLine();
              
              String str = br.readLine();
              StringBuffer sb = new StringBuffer(str);
              sb.append(s2+s);
              String y = sb.toString();
              System.out.println(sb);
              System.out.println("Appending");
              try {
                  PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath+"ALL_6.txt", true)));
                  try {
                      out.println(y);
                  } finally {
                      out.close();
                  }
              } catch (IOException e) {
                                               } 
              
              
              System.out.println("Done");
              n1++;

         }catch(IOException e) {}

    
    }
    
}