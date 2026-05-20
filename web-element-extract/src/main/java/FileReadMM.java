

import java.io.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author vipin
 */
public class FileReadMM {

    private Scanner s;
    List<String> temps = new ArrayList<>();
    SimpleDateFormat sdformat = new SimpleDateFormat("MM/dd/yyyy");
    StringBuilder stringBuilder = new StringBuilder();


    void openFile(String fp) {
        try {
            s = new Scanner(new File(fp));
        } catch (FileNotFoundException ex) {
            System.out.println("File Not Found" + ex);
        }

    }


    public List<String> readFile() {
        s.useDelimiter(">|<| |\"|a|class=|prevDrawItem|href=|h5|/li|stNum1|stNum2|stNum3|stNum4|stNum5");
        while (s.hasNext()) {
            String a = s.next();
            if (!a.matches("^[a-zA-Z]*$")) {

                if (!a.startsWith("/")) {
                    if (isValidDate(a)) {
                        temps.add(stringBuilder.toString());
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(a);
                    } else if (isNumeric(a)) {
                        stringBuilder.append("  ");
                        stringBuilder.append(a);

                    }
                }
            }
        }
        s.close();
        temps.removeAll(Arrays.asList("", null));
        return temps;
    }

    private boolean isNumeric(String a) {

        if (a == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(a);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private boolean isValidDate(String a) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("mm/dd/yyyy");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(a.trim());
        } catch (ParseException pe) {
            return false;
        }
        return true;

    }


    public List<String> reverseArrayList(List<String> alist) {
        ArrayList<String> revArrayList = new ArrayList<String>();
        for (int i = alist.size() - 1; i >= 0; i--) {
            revArrayList.add(alist.get(i));
        }

        return revArrayList;
    }

    void printArray() throws ParseException {
        System.out.println("Inside printArray");
        List<String> strings = reverseArrayList(temps);
        int j = 0;
        for (String s : strings) {
            j++;
            setLineDto(s);
            System.out.println(s);
        }

    }

    public String setLineDto(String x) throws ParseException {

        Integer[] temp1 = new Integer[5];
        String[] temp = x.split("  ");
        java.util.Date dt = sdformat.parse(temp[0]);
        temp1[0] = Integer.parseInt(temp[1]);
        temp1[1] = Integer.parseInt(temp[2]);
        temp1[2] = Integer.parseInt(temp[3]);
        temp1[3] = Integer.parseInt(temp[4]);
        temp1[4] = Integer.parseInt(temp[5]);
        Arrays.sort(temp1);
        String q = temp[0] + "  " + temp1[0] + "  " + temp1[1] + "  " + temp1[2] + "  " + temp1[3] + "  " + temp1[4] + "  " + temp[6];
        return q;
    }


}
