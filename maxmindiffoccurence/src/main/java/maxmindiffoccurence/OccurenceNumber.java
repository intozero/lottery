package maxmindiffoccurence;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author vipin
 */
@Data
@Getter
@Setter
public class OccurenceNumber {
    
    public  int minDifferenceOccurence;
    public  int maxDifferenceOccurence;
    public  int occurenceSinceLast;
    public  int lastOccureddt;
}
