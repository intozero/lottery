/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package totsincecombined;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * @author vipin
 */
public class ResultDto {

    public static String requestType;
    public static int diffSince = 0;
    public static int totaldraw;
    public static int totaldrawtemp;
    public static LinkedHashMap<Integer, Integer> hmapall;
    public static LinkedHashMap<Integer, Integer> hmapSince;
    public static LinkedHashMap<Integer, ArrayList> trioallmap;
    public static LinkedHashMap<Integer, ArrayList> sortbyTotalmap;//Sorted Value by Total --all three
    public static LinkedHashMap<Integer, ArrayList> sortbySincemap; // Sorted Value by Since Occurence --all three
    public static LinkedHashMap<Integer, Integer> sinceOccurenceRangeTotal;
    public static LinkedHashMap<Integer, Integer> totalOccurenceRangeTotal;

    public static void setResultGroupAll(LinkedHashMap<Integer, Integer> hmapall) {
        ResultDto.hmapall = hmapall;
    }

    public static LinkedHashMap<Integer, Integer> getResultGroupAll() {
        return hmapall;
    }

    public static void setResultSinceAll(LinkedHashMap<Integer, Integer> hmapSince) {
        ResultDto.hmapSince = hmapSince;
    }

    public static LinkedHashMap<Integer, Integer> getSinceGroupAll() {
        return hmapSince;
    }

    public static void settrio(LinkedHashMap<Integer, ArrayList> trioallmap) {
        ResultDto.trioallmap = trioallmap;
    }

    public static LinkedHashMap<Integer, ArrayList> gettrio() {
        return trioallmap;
    }


    public static void setSortbyTotalmap(LinkedHashMap<Integer, ArrayList> sortbyTotalmap) {
        ResultDto.sortbyTotalmap = sortbyTotalmap;
    }

    public static LinkedHashMap<Integer, ArrayList> getSortbyTotalmap() {
        return sortbyTotalmap;
    }

    public static void setSortbySincemap(LinkedHashMap<Integer, ArrayList> sortbySincemap) {
        ResultDto.sortbySincemap = sortbySincemap;
    }

    public static LinkedHashMap<Integer, ArrayList> getSortbySincemap() {
        return sortbySincemap;
    }


    public static void setTotalDraw(int totaldraw) {
        ResultDto.totaldraw = totaldraw;
    }

    public static int getTotalDraw() {
        return totaldraw;
    }

    public static void setTotalDrawTemp(int totaldrawtemp) {
        ResultDto.totaldrawtemp = totaldrawtemp;
    }

    public static int getTotalDrawTemp() {
        return totaldrawtemp;
    }

    public static void setsinceOccurenceRangeTotal(LinkedHashMap<Integer, Integer> sinceOccurenceRangeTotal) {
        ResultDto.sinceOccurenceRangeTotal = sinceOccurenceRangeTotal;
    }

    public static LinkedHashMap<Integer, Integer> getsinceOccurenceRangeTotal() {
        return sinceOccurenceRangeTotal;
    }

    public static void settotalOccurenceRangeTotal(LinkedHashMap<Integer, Integer> totalOccurenceRangeTotal) {
        ResultDto.totalOccurenceRangeTotal = totalOccurenceRangeTotal;
    }

    public static LinkedHashMap<Integer, Integer> gettotalOccurenceRangeTotal() {
        return totalOccurenceRangeTotal;
    }


}
