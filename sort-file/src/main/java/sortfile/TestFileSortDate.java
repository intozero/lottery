package sortfile;

import org.junit.Test;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class TestFileSortDate {

    public static String filePath = System.getProperty("user.home") + "/Documents/Projects/JavaProjects/General/lottery/";

    FileReadAndSort fileReadAndSort = new FileReadAndSort();


    @Test
    public void testMethod() throws ParseException {
        filePath = filePath + "pb-unsorted.txt";
        fileReadAndSort.openFile(filePath);
        List<String> listOfLines = fileReadAndSort.reverseFile();
        Collections.reverse(listOfLines);
        listOfLines.forEach((n) -> System.out.println(n));

    }


}
