
package sortfile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

/**
 * @author vipin
 * The module used to read  file from a path and sort it
 */
public class ReadAndSortFile {

    public static PrintWriter printFile;
    public static String filePath = System.getProperty("user.home") + "/Documents/Projects/JavaProjects/General/lottery/";


    public static void main(String[] args) throws ParseException, IOException {

        if ((args[0].length() != 0) && (args[0].equalsIgnoreCase("MM"))) {
            printFile = new PrintWriter(new BufferedWriter(new FileWriter(filePath + "mm-sorted.txt", false)));
            filePath = filePath + "mm.txt";
        } else if ((args[0].length() != 0) && (args[0].equalsIgnoreCase("PB"))) {
            printFile = new PrintWriter(new BufferedWriter(new FileWriter(filePath + "pb-sorted.txt", false)));
            filePath = filePath + "pb.txt";
        } else {

            System.out.println("invalid argument");
        }


        FileReadAndSort fileRead = new FileReadAndSort();
        fileRead.openFile(filePath);
        List<String> listOfLines = fileRead.readFileAndSort();
        Collections.reverse(listOfLines);
        listOfLines.forEach(s -> printFile.println(s));
        printFile.close();

    }
}
