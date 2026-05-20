import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class WebPageExtractor {


    public static void main(String[] args) throws ParseException {


        System.out.println("Enter which lottery MM , PB or F5");
        Scanner scanner = new Scanner(System.in);
        String inputType = scanner.next();

        if (inputType.equalsIgnoreCase("MM")) {

            String fp = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/mm-2024-web.txt";
            FileReadMM fr = new FileReadMM();
            fr.openFile(fp);
            fr.readFile();
            fr.printArray();

        } else if (inputType.equalsIgnoreCase("PB")) {
            String fp = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/pb-2025-web.txt";
            FileReadPB fileReadPB = new FileReadPB();
            fileReadPB.openFile(fp);
            List<String> qq = fileReadPB.readFile();
            Collections.reverse(qq);
//            System.out.println(qq);

            qq.stream().forEach(System.out::println);


        }

    }

}

