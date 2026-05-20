/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package occurencestudy;

import java.text.ParseException;
import java.util.*;

/**
 * @author vipin
 */
public class FirstLastStudyEachOccur {
    private int k;
    public static ArrayList<Integer> wholeList = new ArrayList<Integer>();

    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";


    public static void main(String[] args) throws ParseException {

        System.out.println("Enter either MM or PB");
        Scanner scannerInput = new Scanner(System.in);
        String input = scannerInput.next();

        if (input.equalsIgnoreCase("MM")) {
            filePath = filePath + "mm-sorted.txt";
        } else if (input.equalsIgnoreCase("PB")) {
            filePath = filePath + "pb-sorted.txt";
        } else {

            System.out.println("invalid argument");
        }

        new FirstLastStudyEachOccur().startStudy(filePath);
    }

    private void startStudy(String filePath) throws ParseException {

        FileRead fr = new FileRead();
        fr.openFile(filePath);
        List<LineDto> listlinedto = fr.readFile();
        List<Integer> firstNumbers = new ArrayList<>();
        List<Integer> lastNumbers = new ArrayList<>();
        List<Integer> sumNumbers = new ArrayList<>();

        for (int i = 0; i < listlinedto.size(); i++) {
            if (listlinedto.get(i).getNumberone() == 13) {
                System.out.println(listlinedto.get(i).getNumberfive());
            }

        }


    }


}
