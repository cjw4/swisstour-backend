package dg.swiss.swiss_dg_db.tournament;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TournamentDTO {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String division;

    @NotNull
    private Integer place;

    private Integer rating;

    private Double prize;

    private Integer score;

    private Double points;

    @NotNull
    private Long event;

    @NotNull
    private Long player;

}
