
package sumapplication;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author vipin
 */
@Data
@Getter
@Setter
public class LineDTO {
    private Integer numberone;
    private Integer numbertwo;
    private Integer numberthree;
    private Integer numberfour;
    private Integer numberfive;
    private Integer numbersix;
    private Date lotDate;
}
