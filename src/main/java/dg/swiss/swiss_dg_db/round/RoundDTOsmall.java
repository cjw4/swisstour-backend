package dg.swiss.swiss_dg_db.round;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoundDTOsmall {
    private int roundNumber;
    private int roundRating;
    private int roundScore;
}
