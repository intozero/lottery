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
public class FirstLastStudy {
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

        new FirstLastStudy().startStudy(filePath);
    }

    private void startStudy(String filePath) throws ParseException {

        FileRead fr = new FileRead();
        fr.openFile(filePath);
        List<LineDto> listlinedto = fr.readFile();
        List<Integer> firstNumbers = new ArrayList<>();
        List<Integer> lastNumbers = new ArrayList<>();
        List<Integer> sumNumbers = new ArrayList<>();

        for(int i=0;i< listlinedto.size();i++)
        {
            firstNumbers.add(listlinedto.get(i).getNumberone());
            lastNumbers.add(listlinedto.get(i).getNumberfive());
            sumNumbers.add(listlinedto.get(i).getNumberone()+listlinedto.get(i).getNumberfive());

        }
        System.out.println(firstNumbers);
        System.out.println(lastNumbers);

        // Step 1: Count frequency of each number
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        for (int num : firstNumbers) {
            frequencyMap.put(num, frequencyMap.getOrDefault(num, 0) + 1);
        }

        // Step 2: Group numbers by frequency
        Map<Integer, List<Integer>> groupedByFrequency = new TreeMap<>();
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            int number = entry.getKey();
            int freq = entry.getValue();
            groupedByFrequency
                    .computeIfAbsent(freq, k -> new ArrayList<>())
                    .add(number);
        }

        // Step 3: Print result
        for (Map.Entry<Integer, List<Integer>> entry : groupedByFrequency.entrySet()) {
            int freq = entry.getKey();
            List<Integer> nums = entry.getValue();
            System.out.println(freq + (freq == 1 ? " occurrence: " : " occurrences: ") + nums);
        }

        System.out.println("************************************************************************");
        // Step 1: Count frequency of each number
        frequencyMap.clear();
        for (int num : lastNumbers) {
            frequencyMap.put(num, frequencyMap.getOrDefault(num, 0) + 1);
        }

        // Step 2: Group numbers by frequency
        groupedByFrequency.clear();
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            int number = entry.getKey();
            int freq = entry.getValue();
            groupedByFrequency
                    .computeIfAbsent(freq, k -> new ArrayList<>())
                    .add(number);
        }

        // Step 3: Print result
        for (Map.Entry<Integer, List<Integer>> entry : groupedByFrequency.entrySet()) {
            int freq = entry.getKey();
            List<Integer> nums = entry.getValue();
            System.out.println(freq + (freq == 1 ? " occurrence: " : " occurrences: ") + nums);
        }

        System.out.println("************************************************************************");
        // Step 1: Count frequency of each number
        frequencyMap.clear();
        for (int num : sumNumbers) {
            frequencyMap.put(num, frequencyMap.getOrDefault(num, 0) + 1);
        }

        // Step 2: Group numbers by frequency
        groupedByFrequency.clear();
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            int number = entry.getKey();
            int freq = entry.getValue();
            groupedByFrequency
                    .computeIfAbsent(freq, k -> new ArrayList<>())
                    .add(number);
        }

        // Step 3: Print result
        for (Map.Entry<Integer, List<Integer>> entry : groupedByFrequency.entrySet()) {
            int freq = entry.getKey();
            List<Integer> nums = entry.getValue();
            System.out.println(freq + (freq == 1 ? " occurrence: " : " occurrences: ") + nums);
        }

    }


}
