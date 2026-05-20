/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package groupingrange;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 *
 * @author vipin
 */
@Data
@Setter
@Getter
public class LineDTO {
    private Integer numberone;
    private Integer numbertwo;
    private Integer numberthree;
    private Integer numberfour;
    private Integer numberfive;
    private Integer numbersix;
    private Date lotDate;
}