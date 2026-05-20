/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package totsincecombined;

import grouping.CountNumberOfOccurrenceOfWhiteBalls;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author vipin
 */
public class CountNumberOfOccurrenceOfWhiteBallsDelegate {


    //TODO - Is multithreading really being implemented?
    //Just delegate the run to CountNumberOfOccurrenceOfWhiteBalls thread and the result in hash map in ResultDto
    public void groupingDelegate(String fp) throws ParseException, InterruptedException, IOException {

        FileRead fileRead = new FileRead();
        fileRead.openFile(fp);
        List<LineDto> allLines = fileRead.readFile();
        Thread thread = new Thread(new CountNumberOfOccurrenceOfWhiteBalls(allLines));
        thread.start();

        while (thread.isAlive()) {
        }
    }
}
