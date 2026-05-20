
package totsincecombined;

import java.io.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author vipin
 */
public class FileRead {

    public FileRead() {


    }


    private Scanner s;
    //private String arraystyle[] = new String[10000];
    List<String> temps = new ArrayList<String>();
    SimpleDateFormat sdformat = new SimpleDateFormat("MM/dd/yyyy");

    int i = 0;
    int w = 10;
    int z = 0;


    public void openFile(String fp) {
        try {
            s = new Scanner(new File(fp));


        } catch (FileNotFoundException ex) {
            System.out.println("File Not Found" + ex);


        }

    }


    public List<LineDto> readFile() throws ParseException {
        s.useDelimiter("\\n");
        List<LineDto> listlinedto = new ArrayList<LineDto>();
// ResultDto.totaldraw = w;
// System.out.println( " ResultDto.totaldraw  " + ResultDto.totaldraw);
// 
        while (s.hasNext()) {
            z++;
            String a = s.next();
//
//            if(i%2== 0 ) {
//                LineDto linedto = setLineDto(a);
//                listlinedto.add( linedto);
//            }
//            i = i + 1;


            LineDto linedto = setLineDto(a);
            listlinedto.add( linedto);

            if (z == ResultDto.getTotalDraw()) {
                //  System.out.println(listlinedto.get(i-1).getLotDate() + "     " + z);
                break;

            }


        }
        s.close();


        return listlinedto;

    }


    public LineDto setLineDto(String x) throws ParseException {


        String[] temp = x.split("  ");


        java.util.Date dt = sdformat.parse(temp[0]);

        LineDto ldto = new LineDto();

        ldto.setLotDate(dt);
        ldto.setNumberone(Integer.valueOf(temp[1]));
        ldto.setNumbertwo(Integer.valueOf(temp[2]));
        ldto.setNumberthree(Integer.valueOf(temp[3]));
        ldto.setNumberfour(Integer.valueOf(temp[4]));
        // ldto.setNumberfive(Integer.valueOf(temp[5]));
        if (temp.length == 5) {
            ldto.setNumberfive(99);
            ldto.setNumbersix(99);
            return ldto;
        } else ldto.setNumberfive(Integer.valueOf(temp[5]));

        if (temp.length == 6) {
            ldto.setNumbersix(99);
        } else ldto.setNumbersix(Integer.valueOf(temp[6]));

        return ldto;

    }


}
