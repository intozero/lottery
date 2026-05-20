package groupingrange;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * @author vipin
 */


public class GroupTheRange {

    public static String filePath = System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/";

    HashMap<Integer, Integer> hmaprange = new HashMap<Integer, Integer>();
    int range;


    public static void main(String[] args) throws ParseException, IOException {

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

        new GroupTheRange().groupingRangeStarts(filePath);
    }


    public void groupingRangeStarts(String fb) throws ParseException {

        FileRead fr = new FileRead();
        fr.openFile(fb);
        List<LineDTO> ldto = fr.readFile();
        groupingRangeLogic(ldto);

    }

    private void groupingRangeLogic(List<LineDTO> ldto) throws ParseException {

        HashMap<String, Integer> hmap = new HashMap<String, Integer>();
        HashMap<Integer, Integer> hmapsquare = new HashMap<Integer, Integer>();
        HashMap<Integer, String> hmaptotalinfo = new HashMap<Integer, String>();
        HashMap<Integer, String> hmaprangeinfo = new HashMap<Integer, String>();

        hmaptotalinfo.put(5, " 5 - Single                  ");
        hmaptotalinfo.put(7, " 1 - Double & 3 - Single     ");
        hmaptotalinfo.put(9, " 2 - Double &  1 - Single    ");
        hmaptotalinfo.put(11, " 1 - Triple &  2 - Single    ");
        hmaptotalinfo.put(13, " 1 - Triple & 1 - Double     ");
        hmaptotalinfo.put(17, " 1 - Quadruple & 1 - Single  ");
        hmaptotalinfo.put(25, " 1 - Quintuple               ");

        hmaprangeinfo.put(2, " 1 X 0-9");
        hmaprangeinfo.put(8, " 2 X 0-9");
        hmaprangeinfo.put(18, " 3 X 0-9");
        hmaprangeinfo.put(32, " 4 X 0-9");
        hmaprangeinfo.put(50, " 5 X 0-9");

        hmaprangeinfo.put(5, " 1 X 10-19");
        hmaprangeinfo.put(20, " 2 X 10-19");
        hmaprangeinfo.put(45, " 3 X 10-19");
        hmaprangeinfo.put(80, " 4 X 10-19");
        hmaprangeinfo.put(125, " 5 X 10-19");

        hmaprangeinfo.put(10, " 1 X 20-29");
        hmaprangeinfo.put(40, " 2 X 20-29");
        hmaprangeinfo.put(90, " 3 X 20-29");
        hmaprangeinfo.put(160, " 4 X 20-29");
        hmaprangeinfo.put(250, " 5 X 20-29");


        hmaprangeinfo.put(17, " 1 X 30-39");
        hmaprangeinfo.put(68, " 2 X 30-39");
        hmaprangeinfo.put(153, " 3 X 30-39");
        hmaprangeinfo.put(272, " 4 X 30-39");
        hmaprangeinfo.put(2125, " 5 X 30-39");


        hmaprangeinfo.put(26, " 1 X 40-49");
        hmaprangeinfo.put(104, " 2 X 40-49");
        hmaprangeinfo.put(234, " 3 X 40-49");
        hmaprangeinfo.put(416, " 4 X 40-49");
        hmaprangeinfo.put(3250, " 5 X 40-49");

        hmaprangeinfo.put(37, " 1 X 50-59");
        hmaprangeinfo.put(148, " 2 X 50-59");
        hmaprangeinfo.put(333, " 3 X 50-59");
        hmaprangeinfo.put(592, " 4 X 50-59");
        hmaprangeinfo.put(4625, " 5 X 50-59");


        hmaprangeinfo.put(50, " 1 X 60-69");
        hmaprangeinfo.put(200, " 2 X 60-69");
        hmaprangeinfo.put(450, " 3 X 60-69");
        hmaprangeinfo.put(800, " 4 X 60-69");
        hmaprangeinfo.put(6250, " 5 X 60-69");

        hmaprangeinfo.put(65, " 1 X 70-79");
        hmaprangeinfo.put(260, " 2 X 70-79");
        hmaprangeinfo.put(585, " 3 X 70-79");
        hmaprangeinfo.put(1040, " 4 X 70-79");
        hmaprangeinfo.put(8125, " 5 X 70-79");


        for (LineDTO lto : ldto) {
            int i1 = 0, i2 = 0, i3 = 0, i4 = 0, i5 = 0, i6 = 0, i7 = 0, i8 = 0;

            Integer[] Inti = new Integer[5];
            Inti[0] = lto.getNumberone();
            Inti[1] = lto.getNumbertwo();
            Inti[2] = lto.getNumberthree();
            Inti[3] = lto.getNumberfour();
            Inti[4] = lto.getNumberfive();

            for (Integer x : Inti) {
                if (x < 10 && x > 0) {
                    i1++;
                } else if (x >= 10 && x < 20) {
                    i2++;
                } else if (x >= 20 && x < 30) {
                    i3++;
                } else if (x >= 30 && x < 40) {
                    i4++;
                } else if (x >= 40 && x < 50) {
                    i5++;
                } else if (x >= 50 && x < 60) {
                    i6++;
                } else if (x >= 60 && x < 70) {
                    i7++;
                } else if (x >= 70 && x < 80) {
                    i8++;
                }
            }
            String Fin = "I " + i1 + "  " + "II " + i2 + "  " + "III " + i3 + "  " + "IV " + i4 + "  " + "V " + i5 + "  " + "VI " + i6 + "  " + "VII " + i7 + "  " + "VIII " + i8 + "  ";
            int sumsquare = i1 * i1 + i2 * i2 + i3 * i3 + i4 * i4 + i5 * i5 + i6 * i6 + i7 * i7 + i8 * i8;

            for (int w = 1; w <= 8; w++) {
                switch (w) {

                    case 1:
                        range = 2 * i1 * i1;
                        functioncallforrange(range);
                        break;
                    case 2:
                        range = 5 * i2 * i2;
                        functioncallforrange(range);
                        break;
                    case 3:
                        range = 10 * i3 * i3;
                        functioncallforrange(range);
                        break;
                    case 4:
                        range = 17 * i4 * i4;
                        functioncallforrange(range);
                        break;
                    case 5:
                        range = 26 * i5 * i5;
                        functioncallforrange(range);
                        break;
                    case 6:
                        range = 37 * i6 * i6;
                        functioncallforrange(range);
                        break;
                    case 7:
                        range = 50 * i7 * i7;
                        functioncallforrange(range);
                        break;
                    case 8:
                        range = 65 * i8 * i8;
                        functioncallforrange(range);
                        break;


                }


            }


            if (hmap.containsKey(Fin)) {
                Integer q = hmap.get(Fin) + 1;
                hmap.put(Fin, q);
            } else {

                hmap.put(Fin, 1);
            }


            if (hmapsquare.containsKey(sumsquare)) {
                Integer q = hmapsquare.get(sumsquare) + 1;
                hmapsquare.put(sumsquare, q);
            } else {

                hmapsquare.put(sumsquare, 1);
            }


        }

//        System.out.println("*******************RANGE INTEREST*************************");
        HashMap<String, Integer> hmap1 = sortByValues(hmap);
        // System.out.println("Grouping Range Arranged" +  "\t********************"+ hmap1) ;
        List<String> stringsCurrent = new ArrayList<>();
        for (String key : hmap1.keySet()) {
//            System.out.println(key + "\t" + hmap1.get(key));
            if(isKeyValid(key))
            {
//                key=  key.replaceAll("\\s", "");
                key = key.trim();
                stringsCurrent.add(key);
//                System.out.println(key);
            }

        }


        FileRead fr = new FileRead();
        fr.openFile(System.getProperty("user.home") + "/Documents/may-2024-documents/Projects/JavaProjects/General/lottery/" + "mm-all-range-combination.txt");
        HashMap<String, Integer> stringIntegerHashMap = fr.readRangefile();
        List<String> stringsAll = new ArrayList<>();
//        System.out.println("************************************************************************************************************");
        for (String key : stringIntegerHashMap.keySet()) {
//            System.out.println(key + "\t" + stringIntegerHashMap.get(key));

            if(isKeyValid(key))
            {

//                key= key.replaceAll("\\s", "");
//                System.out.println(key);
                stringsAll.add(key);
            }
        }

        stringsAll.removeAll(stringsCurrent);
        System.out.println("******************************************************************************************************************************");
        stringsAll.forEach(System.out::println);


    }

    private boolean isKeyValid(String key) {

        String[] temp = key.split("  ");
        String[] v0 = temp[0].split(" ");
        String[] v1 = temp[1].split(" ");
        String[] v2 = temp[2].split(" ");
        String[] v3 = temp[3].split(" ");
        String[] v4 = temp[4].split(" ");
        String[] v5 = temp[5].split(" ");
        String[] v6 = temp[6].split(" ");
        String[] v7 = temp[7].split(" ");

        Integer sum = Integer.valueOf(v0[1]) * Integer.valueOf(v0[1]) +
                Integer.valueOf(v1[1]) * Integer.valueOf(v1[1]) +
                Integer.valueOf(v2[1]) * Integer.valueOf(v2[1]) +
                Integer.valueOf(v3[1]) * Integer.valueOf(v3[1]) +
                Integer.valueOf(v4[1]) * Integer.valueOf(v4[1]) +
                Integer.valueOf(v5[1]) * Integer.valueOf(v5[1]) +
                Integer.valueOf(v6[1]) * Integer.valueOf(v6[1]) +
                Integer.valueOf(v7[1]) * Integer.valueOf(v7[1]);

        if (sum == 7)
        {
            return true;
        }

        return false;
    }

    private HashMap<String, Integer> sortByValues(HashMap<String, Integer> hmap) {
        List list = new LinkedList(hmap.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;


    }

    private HashMap<Integer, Integer> sortByValues_Int_Int(HashMap<Integer, Integer> hmap) {
        List list = new LinkedList(hmap.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;


    }


    private void functioncallforrange(int x) {
        if (hmaprange.containsKey(x)) {
            Integer q = hmaprange.get(x) + 1;
            hmaprange.put(x, q);
        } else {

            hmaprange.put(x, 1);
        }

    }

}
