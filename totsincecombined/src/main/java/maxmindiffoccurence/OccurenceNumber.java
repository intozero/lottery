/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package maxmindiffoccurence;

/**
 *
 * @author vipin
 */
public class OccurenceNumber {
    
    public  int minDifferenceOccurence;
    public  int maxDifferenceOccurence;
    public  int occurenceSinceLast;
    public  int lastOccureddt;

    public int getLastOccureddt() {
        return lastOccureddt;
    }

    public void setLastOccureddt(int lastOccureddt) {
        this.lastOccureddt = lastOccureddt;
    }
    public  int getMinDifferenceOccurence() {
        return minDifferenceOccurence;
    }

    public  int getMaxDifferenceOccurence() {
        return maxDifferenceOccurence;
    }

    public  int getOccurenceSinceLast() {
        return occurenceSinceLast;
    }

    public  void setMinDifferenceOccurence(int minDifferenceOccurence) {
        this.minDifferenceOccurence = minDifferenceOccurence;
    }

    public  void setMaxDifferenceOccurence(int maxDifferenceOccurence) {
        this.maxDifferenceOccurence = maxDifferenceOccurence;
    }

    public  void setOccurenceSinceLasdt(int occurenceSinceLast) {
        this.occurenceSinceLast = occurenceSinceLast;
    }
    
}
