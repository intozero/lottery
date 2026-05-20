/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package combinations;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author vipin
 */
public class GetAllCombinationsInRangeForm {

    static int[] Q;

    static GROUPINGRANGE groupingrange = new GROUPINGRANGE();
    static List<LineDto> lineDtos = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException, FileNotFoundException, IOException {

        System.out.println("Enter which lottery MM , PB ");
        Scanner scanner = new Scanner(System.in);
        String inputType = scanner.next();


        if (inputType.equalsIgnoreCase("MM")) {
            Q = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                    26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                    51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70};
        }

        if (inputType.equalsIgnoreCase("PB")) {
            Q = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                    26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
                    51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69};
        }

        int[] v = new int[100];
        int n = Q.length;
        int k = 5;

        // The deviation for which the combinations have to be figured
        int d = 21;
        int maxK = 5;

        //The sum for which the combinations have to be figured
        int s = 188;

        combinations(v, 1, n, 1, maxK, s, d);
        groupingrange.groupingRangeLogic(lineDtos);

    }

    private static void combinations(int v[], int start, int n, int k1, int maxK, int s, int d) throws InterruptedException {
//        int[] Q = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
//                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
//                51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75};
        int i;
        int sum = 0;
        int deviation = 0;
        int mean = 0;
        if (k1 > maxK) {
            for (i = 1; i <= maxK; i++) {
                sum = sum + Q[v[i] - 1];

            }
            mean = sum / maxK;

            for (i = 1; i <= maxK; i++) {
                deviation = deviation + (int) Math.pow(mean - Q[v[i] - 1], 2);

            }

            deviation = (int) Math.sqrt(deviation / 5);


//            if (deviation == d && sum == s) {
//                for (i = 1; i <= maxK; i++) {
//                    System.out.print("   " + Q[v[i] - 1]);
//
//                }
//
//                System.out.println("   ");
//
//            }

            LineDto lineDto = new LineDto();
            for (i = 1; i <= maxK; i++) {
//                System.out.print("   " + Q[v[i] - 1]);
                if(i==1)
                {
                    lineDto.setNumberone(Q[v[i] - 1]);
                }
                if(i==2)
                {
                    lineDto.setNumbertwo(Q[v[i] - 1]);
                }
                if(i==3)
                {
                    lineDto.setNumberthree(Q[v[i] - 1]);
                }
                if(i==4)
                {
                    lineDto.setNumberfour(Q[v[i] - 1]);
                }
                if(i==5)
                {
                    lineDto.setNumberfive(Q[v[i] - 1]);
                }

            }

            lineDtos.add(lineDto);




//            System.out.println("   ");


            return;
        }
        for (i = start; i <= n; i++) {
            v[k1] = i;
            combinations(v, i + 1, n, k1 + 1, maxK, s, d);
        }




    }


}
