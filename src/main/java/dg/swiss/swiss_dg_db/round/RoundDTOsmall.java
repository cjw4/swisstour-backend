package dg.swiss.swiss_dg_db.round;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoundDTOsmall {
    private Integer roundNumber;
    private Integer roundRating;
    private Integer roundScore;
}
