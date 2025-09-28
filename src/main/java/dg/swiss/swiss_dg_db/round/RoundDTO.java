package dg.swiss.swiss_dg_db.round;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoundDTO {

    private Long id;

    private Integer roundNumber;

    private Integer score;

    private Integer rating;

    @NotNull
    private Long tournament;

}
