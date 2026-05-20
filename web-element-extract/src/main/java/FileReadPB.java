import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author vipin
 */
public class FileReadPB {

    private Scanner s;
    List<String> temps = new ArrayList<String>();
    SimpleDateFormat sdformat = new SimpleDateFormat("MM/dd/yyyy");

    int i = 0;


    void openFile(String fp) {
        try {

            s = new Scanner(new File(fp));
        } catch (FileNotFoundException ex) {
            System.out.println("File Not Found" + ex);


        }

    }


    public List<String> readFile() throws ParseException {

        s.useDelimiter("\\n");

        final String OLD_FORMAT = "EEE, MMM dd, yyyy";
        final String NEW_FORMAT = "M/d/yyyy";

        String resultSet = null;


        List<String> ls = new ArrayList<String>();

        List<String> temp = new ArrayList<String>();
        int i = 0;
        while (s.hasNext()) {
            String line=s.nextLine();


            if(line.contains("card-title"))
            {

                String trimmedLine = line.trim();
                String result = trimmedLine.substring(23,39);

                SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
                Date d = sdf.parse(result);
                sdf.applyPattern(NEW_FORMAT);
                String newDateString = sdf.format(d);
//                System.out.println(newDateString);
                resultSet = newDateString + "  ";

            }
            if(line.contains("col white-balls"))
            {
                String result = line.substring(59).split("<")[0];
                resultSet = resultSet + result + "  ";
//                System.out.println(result);

            }
            if(line.contains("col powerball"))
            {
                String result = line.substring(57).split("<")[0];
                resultSet = resultSet + result ;
//                System.out.println(resultSet);
//                System.out.println();
                ls.add(resultSet);
                resultSet= null;
            }

//            System.out.println();

//            if (i == 8) {
//                temp.remove(7);
//
//                String oldDate = temp.get(0);
//                temp.remove(0);
                String newDateString;
//
//                SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
//                Date d = sdf.parse(oldDate);
//                sdf.applyPattern(NEW_FORMAT);
//                newDateString = sdf.format(d);
//                System.out.print(newDateString);
//
//
//                temp.forEach(value -> System.out.print("  " + Integer.valueOf(value)));
//                System.out.println();
//                temp.clear();
//                i = 0;
//            } else {
//                temp.add(s.next());
//                i++;
//            }


        }
        s.close();
        return ls;


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

    void printArray() {
        System.out.println("Inside printArray");
        String[] tempsArray = temps.toArray(new String[0]);
        int j = 0;
        for (String s : tempsArray) {
            j++;
            System.out.println(s);

            System.out.println();
        }
// System.out.println("total number of elements in the array"+j);

        //System.out.println("Return code from DataDto " + c);
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
