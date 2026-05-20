
package deviation;

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

    private Scanner s;

    SimpleDateFormat sdformat = new SimpleDateFormat("MM/dd/yyyy");

    int i = 0;


    void openFile(String fp) {
        try {
            s = new Scanner(new File(fp));

        } catch (FileNotFoundException ex) {
            System.out.println("File Not Found" + ex);

        }

    }


    public List<LineDTO> readFile() throws ParseException {
        s.useDelimiter("\\n");
        List<LineDTO> listlinedto = new ArrayList<>();
        while (s.hasNext()) {
            String a = s.next();
            LineDTO linedto = setLineDto(a);
            listlinedto.add(i, linedto);

            i = i + 1;

        }
        s.close();
        return listlinedto;

    }


    public LineDTO setLineDto(String x) throws ParseException {

        String[] temp = x.split("  ");
        java.util.Date dt = sdformat.parse(temp[0]);

        LineDTO lineDTO = new LineDTO();
        lineDTO.setLotDate(dt);
        lineDTO.setNumberone(Integer.valueOf(temp[1]));
        lineDTO.setNumbertwo(Integer.valueOf(temp[2]));
        lineDTO.setNumberthree(Integer.valueOf(temp[3]));
        lineDTO.setNumberfour(Integer.valueOf(temp[4]));
        lineDTO.setNumberfive(Integer.valueOf(temp[5]));
        lineDTO.setNumbersix(Integer.valueOf(temp[6]));

        return lineDTO;

    }


}
